import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.jibble.pircbot.*;


public class WordGameBot extends PircBot {
    HashMap<String, Game> games;
    HashMap<String, String> admindetails;
    String adminpass, savefolder;
    
    final Integer MAXMSGLENGTH = 384;
    final String MSG_NOTREGISTERED = "You do not have an account yet or have not signed in properly. Use !wgsignup to sign up for an account.";
    final String MSG_MAXWORDSREACHED = "Uh oh! You seem to have reached the maximum amount of unset words. Your unset word will now be given to someone else at random.";
    final String MSG_WGINFO = "This is JWordGame, an irc game based on (accidentally) guessing words set by others. Type !wghelp for more help.";
    final String MSG_NOGAME = "There is no game in progress on this channel (yet).";
    final String MSG_WGHELP1 = "Type !wgsignup to sign up for the wordgame, no extra details are necessary.";
    final String MSG_WGHELP2 = "Other available commands: !wgpoints !wgstatus !wgdonate !wgdefaultchannel !wgtop, and PM-only: set listwords pwd login";
    final String MSG_WGHELPADMIN =  "Available commands: !wgjoin, !wgnewgame, !wglistgames, !wggiveword !wgsave !wgload !wgsafequit !wgresetword !wgautosave !wgoverridepwd !wgresetallpoints";
    final String MSG_SIGNUPSUCCESS = "You have succefully signed up for the wordgame. A PM has been sent to you with additional information.";
    final String MSG_SIGNUPINTRO = "Welcome to the word game. This game works by setting and guessing certain words during regular IRC conversation. Any sentence you type in a channel participating in the game can cause a set word to be guessed. Both the guesser and setter of the word receive points when this happens, and the guesser will be able to set a word of his own. As a word setter, your goal is to mention your word as often as you can without drawing suspicion. You can get one mention per sentence, actions (/me) are also parsed for mentions. Mentioning your word as part of another word is valid. The more mentions you manage to squeeze in before your word is guessed, the more points you will get when this finally occurs. You can use the listwords command to keep track of your current set words and it will also show you how much mentions you have accumulated so far. Please be aware that unset words expire after a day, don't forget to set them! This game works best if everyone plays it fairly. Please do not make excessive guessing attempts, copy/paste what others say, or partake in wintrading. You get the most fun by guessing the word during regular conversations or making educated guesses. Now go and guess your first word! Tip: Set a password with !wgpwd so you can retrieve your account should your connection details change.";
    final String MSG_SIGNUPFAIL = "A user with that nickname already exists.";
    final String MSG_WANTDEFAULTCHANNEL = "If you want to use a default channel, type !wgdefaultchannel in the channel you would like to use as your default channel.";
    final String MSG_WORDSET = "Word set.";
    final String MSG_NOUNSETWORDS = "You do not have any unset words.";
    final String MSG_DONATED = "Word donated.";
    final String MSG_DEFAULTCHANNEL = "Your default channel has been set to: ";
    final String MSG_INVALIDNUMBER = "Please provide a valid number.";
    final String MSG_NOSUCHUSER = "No such user could be found.";
    final String MSG_NOSUCHGAME = "No such game could be found.";
    final String MSG_NOSUCHWORD = "No such word could be found.";
    final String MSG_WORDRESET = "Word reset";
    final String MSG_SAVEERROR = "Error occured while saving. See stacktrace in STDERR.";
    
    public WordGameBot(HashMap<String, Game> games) {
        this.games = games;
        admindetails = new HashMap<String, String>();
        adminpass = null;
        savefolder = "saves/";
        
        setName("JWG");
        setLogin("JWordGame");
        setVersion("JWordGame BETA by Derecho.");
        setMessageDelay(750);
        sendRawLineViaQueue("MODE " + getNick() + " +B");  // A Sector5d.org thing.
    }
    
    // Inherited methods
    
    public void onMessage(String channel, String sender,
            String login, String hostname, String message) {
        // A message has been received, parse it.
        
        redistributeExpiredWords();
        
        // Get the game if there is one running
        Game game = games.get(getServer() + " " + channel);
        
        // Get the user if (s)he is registered
        User user = getUser(game, sender, login, hostname);
        
        // First try all the commands
        
        Command command;
        if(game != null) {
            command = Commands.toCommand(message, game.commandprefix, getNick());
        }
        else {
            command = Commands.toCommand(message, "!", getNick());
        }
        
        switch(command.command) {
        case WG:
            if(command.arguments.length == 2) { WGInfo(channel, command.arguments[1]); }
            else { WGInfo(channel, sender); }
            break;
        case WGHELP:
            if(command.arguments.length == 2) { WGHelp(channel, command.arguments[1]); }
            else { WGHelp(channel, sender); }
            break;
        case WGSIGNUP:
            WGSignup(channel, sender, login, hostname);
            break;
        case WGPOINTS:
            if(user != null) { WGPoints(channel, user); }
            else { tellNotRegistered(channel, sender);  }
            break;
        case WGSTATUS:
            WGStatus(channel, sender);
            break;
        case WGDONATE:
            if(user != null) { WGDonate(channel, user, game, command);  }
            else { tellNotRegistered(channel, sender);  }
            break;
        case WGDEFAULTCHANNEL:
            if(user != null) { WGDefaultChannel(channel, user, command);    }
            else { tellNotRegistered(channel, sender);  }
            break;
        case WGTOP:
            WGTop(channel, sender, game, command);
            break;
        case WGSET:
        case WGLISTWORDS:
            sendMessage(channel, sender + ": You can only do this in a PM.");
            break;
        }
        
        // Now, let's see if a set word was mentioned (by a signed-up user)
        parseLine(user, game, channel, message);
    }
    
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
        redistributeExpiredWords();

        Command command;
        command = Commands.toCommand(message, null, null);
        
        switch(command.command) {
        case WG:
            WGInfo(sender, null);
            break;
        case WGHELP:
            WGHelp(sender, null);
            break;
        case WGSET:
            WGSet(sender, login, hostname, command);
            break;
        case WGLISTWORDS:
            WGListWords(sender, login, hostname, command);
            break;
        case WGPWD:
            WGPwd(sender, login, hostname, command);
            break;
        case WGLOGIN:
            WGLogin(sender, login, hostname, command);
            break;
        case WGADMIN:
            WGAdmin(sender, login, hostname, command);
            break;
        }
        
        if(isAdmin(sender, login, hostname)) {
            switch(command.command) {
            case WGADMINHELP:
                sendMessage(sender, MSG_WGHELPADMIN);
                break;
            case WGJOIN:
                WGJoin(sender, command);
                break;
            case WGNEWGAME:
                WGNewGame(sender, command);
                break;
            case WGLISTGAMES:
                WGListGames(sender);
                break;
            case WGGIVEWORD:
                WGGiveWord(sender, command);
                break;
            case WGSAVE:
                WGSave(sender, command);
                break;
            case WGLOAD:
                WGLoad(sender, command);
                break;
            case WGSAFEQUIT:
                WGSafeQuit(sender);
                break;
            case WGRESETWORD:
                WGResetWord(sender, command);
                break;
            case WGAUTOSAVE:
                WGAutoSave(sender, command);
                break;
            case WGOVERRIDEPWD:
                WGOverridePwd(sender, command);
                break;
            case WGRESETALLPOINTS:
                WGResetAllPoints(sender, command);
                break;
            }
        }
    }

    public void onAction(String sender, String login, String hostname, String target, String action) {
        // Treat an action as a normal message.
        redistributeExpiredWords();
        Game game = games.get(getServer() + " " + target);
        User user = getUser(game, sender, login, hostname);
        parseLine(user, game, target, action);
    }
    
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        // Autojoin on kick
        if(recipientNick.equalsIgnoreCase(getNick())) {
            joinChannel(channel);
        }
    }
    
    public void onDisconnect() {
        // Autoreconnect with 30 second pauses between retries.
        while(!isConnected()) {
            try {
                reconnect();
            }
            catch(Exception e1) {
                try {
                    Thread.sleep(30000);
                } catch (Exception e2) {
                    // Do nothing.
                }
            }
        }
    }
    
    // Own methods
    
    public Game getGame(String gameid) {
        for(String servchan : games.keySet()) {
            if(gameid.equals(games.get(servchan).id)) {
                return games.get(servchan);
            }
        }
        return null;
    }
    
    public User getUser(Game game, String sender, String login, String hostname) {
        if(game == null) {
            return null;
        }
        
        return game.getUser(sender, login, hostname);
    }
    
    public User getUserByNick(Game game, String nick) {
        if(game == null) {
            return null;
        }
        
        return game.getUserByNick(nick);
    }

    public User getRandomUser(Game game, User exclude) {
        Random generator = new Random();
        Integer randomint = generator.nextInt(game.users.size());
        Integer i = 0;
        for(User randomuser : game.users) {
            if(i == randomint) {
                if(!randomuser.equals(exclude)) {
                    return randomuser;
                }
                else {
                    break;
                }
            }
            i++;
        }
        return getRandomUser(game, exclude);
    }
    
    public void sendMessageWrapper(String recipient, String nick, String message) {
        // Split the string first into mutliple lines
        StringTokenizer token = new StringTokenizer(message, " ");
        StringBuilder chunk = new StringBuilder(MAXMSGLENGTH);
        ArrayList<String> lines = new ArrayList<String>();
        while (token.hasMoreTokens()) {
            String word = token.nextToken();

            if (chunk.length() + word.length() > MAXMSGLENGTH) {
                lines.add(chunk.toString());
                chunk.delete(0, chunk.length());
            }
            chunk.append(word + " ");
        }
        lines.add(chunk.toString());

        // Send string
        for(String line : lines) {
            if(nick == null) {
                sendMessage(recipient, line);
            }
            else {
                sendMessage(recipient, nick + ": " + line);
            }
        }
    }
    
    public void tellNotRegistered(String channel, String sender) {
        sendMessageWrapper(channel, sender, MSG_NOTREGISTERED);
    }
    
    public boolean isAdmin(String sender, String login, String hostname) {
        return (sender.equals(admindetails.get("nick")) && login.equals(admindetails.get("login")) && hostname.equals(admindetails.get("hostname")));
    }
    
    public void parseLine(User guesser, Game game, String channel, String message) {
        if(guesser != null) {           
            for(User setter : game.users) {             
                Set<Word> words = new HashSet<Word>(setter.wordobjs);
                for(Word word : words) {
                    if(message.contains(word.word)) {
                        if(guesser.equals(setter)) {
                            // User mentioned his own word
                            word.mentions++;
                            if(game.autosave) {
                                saveGame(game);
                            }
                        }
                        else {
                            // User mentioned someone elses word
                            // First calculate the rewards
                            Integer guesserreward = word.calcGuesserReward(game.pointsreference);
                            Integer setterreward = word.calcSetterReward(game.pointsreference);
                            
                            // Inform the guesser about his accomplishment                          
                            sendMessage(channel, guesser + ": Congratulations! You have guessed the word '" + word + "' set by " + setter + "!");
                            sendMessage(channel, "Rewards: " + guesser + " " + guesserreward + " points, " + setter + " " + setterreward + " points.");
                            
                            // Give the guesser and setter a reward
                            guesser.points += guesserreward;
                            setter.points += setterreward;
                            
                            // Remove the word from the setter
                            setter.wordobjs.remove(word);
                            
                            // Assign word to guesser or random person
                            if(guesser.unsetwordobjs.size() <= game.maxwords) {
                                guesser.addUnsetWord();
                                sendMessageWrapper(channel, null, "Word given to: " + guesser);
                            }
                            else {
                                sendMessageWrapper(channel, guesser.nick, MSG_MAXWORDSREACHED);
                                User randomuser = getRandomUser(game, guesser);
                                randomuser.addUnsetWord();
                                sendMessageWrapper(channel, null, "Word given to: " + randomuser);
                            }
                            
                            if(game.autosave) {
                                saveGame(game);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public boolean saveGame(Game game) {
        if(game != null) {
            try {
                // If the directory does not exist yet, create it
                if(!new File(savefolder).exists()) {
                    new File(savefolder).mkdirs();
                }
                
                FileOutputStream fos = new FileOutputStream(savefolder + game.id);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(game);
                oos.close();
                
                return true;
            }
            catch(Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
        else {
            return false;
        }
    }

    public void redistributeExpiredWords() {
        for(Game game : games.values()) {
            for(User user : game.users) {
                if(user.hasUnsetWords() && user.unsetwordobjs.peekFirst().isExpired()) {
                    User randomuser = getRandomUser(game, user);
                    user.removeUnsetWord();
                    randomuser.addUnsetWord();
                    sendMessageWrapper(user.nick, null, "You've hold on to an unset word for 24 hours. The unset word has now been redistributed.");
                    sendMessageWrapper(randomuser.nick, null, "You've been given a redistributed word that was held on to for too long.");
                }
            }
        }
    }
    
    // User commands
    
    public void WGInfo(String recepient, String nick) {
        sendMessageWrapper(recepient, nick, MSG_WGINFO);
    }
    
    public void WGHelp(String recepient, String nick) {
        sendMessageWrapper(recepient, nick, MSG_WGHELP1);
        sendMessageWrapper(recepient, nick, MSG_WGHELP2);
    }
    
    public void WGSignup(String channel, String sender, String login, String hostname) {        
        Game game;
        game = games.get(getServer() + " " + channel);
        
        if(game == null) {
            sendMessageWrapper(channel, sender, MSG_NOGAME);
            return;
        }
        
        if(game.addUser(new User(sender, login, hostname))) {
            sendMessageWrapper(channel, sender, MSG_SIGNUPSUCCESS);
            sendMessageWrapper(sender, null, MSG_SIGNUPINTRO);
            if(game.autosave) {
                saveGame(game);
            }
        }
        else {
            sendMessageWrapper(channel, sender, MSG_SIGNUPFAIL);
        }
    }
    
    public void WGPoints(String channel, User user) {
        sendMessage(channel, user + ": You have " + user.points + " points.");
    }
    
    public void WGStatus(String channel, String sender) {
        Integer setwords = 0;
        String availablewords = new String();
        HashMap<String, Integer> setusers = new HashMap<String, Integer>();
        
        Game game;
        game = games.get(getServer() + " " + channel);
        
        if(game == null) {
            sendMessageWrapper(channel, sender, MSG_NOGAME);
            return;
        }
        
        // The users that have words left to set, are added to a seperate map which will be ordered later on        
        for(User user : game.users) {
            setwords += user.wordobjs.size(); // Also calculate the total of set words
            if(user.hasUnsetWords()) {
                setusers.put(user.nick, user.unsetwordobjs.size());
            }
        }
        
        // Java does not support sorting a map by it's value instead of it's keys, so here comes the solution
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(setusers.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                Integer diff = e2.getValue().compareTo(e1.getValue());
                if(diff == 0) {
                    return e1.getKey().compareTo(e2.getKey());
                }
                else {
                    return diff;
                }
            }
        });
        
        // Now we create the string that will have the users ordered.
        for(Map.Entry<String, Integer> setuser : list) {
            availablewords += setuser.getKey() + " (" + setuser.getValue() + "), ";
        }
        
        sendMessage(channel, sender + ": " + setwords + " words have been set.");
        if(!"".equals(availablewords)) {
            sendMessageWrapper(channel, null, "The following users can set words: " + availablewords.substring(0, availablewords.length()-2));
        }
    }
    
    public void WGSet(String sender, String login, String hostname, Command command) {
        Game game = null;
        User user = null;
        String word = null;
        
        if(command.arguments.length == 3) {
            game = games.get(getServer() + " " + command.arguments[1]);
            user = getUser(game, sender, login, hostname);
            word = command.arguments[2];
        }
        else if(command.arguments.length == 2) {
            for(Game loopgame : games.values()) {
                User loopuser = loopgame.getUser(sender, login, hostname);
                if(loopuser != null) {
                    user = loopuser;
                    game = games.get(getServer() + " " + user.defaultchannel);
                    word = command.arguments[1];
                    
                    if(game == null) {
                        sendMessage(sender, MSG_WANTDEFAULTCHANNEL);
                        sendMessage(sender, "Otherwise, use: !wgset <#channel> <word>");
                        return;
                    }
                }
            }
            if(user == null) {
                tellNotRegistered(sender, null);
                return;
            }
        }
        else {
            sendMessage(sender, "WGSET usage: !wgset <#channel> <word>");
            return;
        }
        
        // We got all the needed data, continue
        if(user == null) {
            tellNotRegistered(sender, null);
        }
        else {
            if(user.setWord(word)) {
                sendMessage(sender, MSG_WORDSET);
                if(game.autosave) {
                    saveGame(game);
                }
            }
            else {
                sendMessage(sender, MSG_NOUNSETWORDS);
            }
        }
    }
    
    public void WGListWords(String sender, String login, String hostname, Command command) {
        Game game = null;
        User user = null;
        
        if(command.arguments.length == 2) {
            game = games.get(getServer() + " " + command.arguments[1]);
            user = getUser(game, sender, login, hostname);
        }
        else if(command.arguments.length == 1) {
            for(Game loopgame : games.values()) {
                User loopuser = loopgame.getUser(sender, login, hostname);
                if(loopuser != null) {
                    user = loopuser;
                    game = games.get(getServer() + " " + user.defaultchannel);
                    if(game == null) {
                        sendMessage(sender, MSG_WANTDEFAULTCHANNEL);
                        sendMessage(sender, "Otherwise, use: !wglistwords <#channel>");
                        return;
                    }
                }
            }
            if(user == null) {
                tellNotRegistered(sender, null);
                return;
            }
        }
        else {
            sendMessage(sender, "WGLISTWORDS usage: !wglistwords <#channel>");
            sendMessage(sender, MSG_WANTDEFAULTCHANNEL);
            return;
        }
        
        // We got all the needed data, continue
        if(user == null) {
            tellNotRegistered(sender, null);
        }
        else {
            sendMessage(sender, user.listWords());
        }
    }
    
    public void WGDonate(String channel, User user, Game game, Command command) {
        if(user.hasUnsetWords()) {
            if(command.arguments.length == 2) {
                User recipient = getUserByNick(game, command.arguments[1]);
                if(recipient == null) {
                    sendMessageWrapper(channel, user.nick, MSG_NOSUCHUSER);
                }
                else {
                    user.removeUnsetWord();
                    recipient.addUnsetWord();
                    sendMessage(channel, MSG_DONATED);
                    if(game.autosave) {
                        saveGame(game);
                    }
                }
            }
            else {
                sendMessage(channel, "WGDONATE usage: !wgdonate <user>");
            }
        }
        else {
            sendMessage(channel, MSG_NOUNSETWORDS);
        }
    }
    
    public void WGDefaultChannel(String channel, User user, Command command) {
        String defaultchannel = channel;
        if(command.arguments.length == 2) {
            defaultchannel = command.arguments[1];
        }
        user.defaultchannel = defaultchannel;
        sendMessageWrapper(channel, user.nick, MSG_DEFAULTCHANNEL + defaultchannel);
    }
    
    public void WGTop(String channel, String sender, Game game, Command command) {
        Integer defaultamount = 3;
        Integer amount = defaultamount;
        if(command.arguments.length == 2) {
            try {
                amount = Integer.parseInt(command.arguments[1]);
            }
            catch(Exception ex) {
                sendMessageWrapper(channel, sender, MSG_INVALIDNUMBER);
            }
        }
        
        TreeMap<Integer, String> topusers = new TreeMap<Integer, String>();
        
        if(game != null) {
            for(User user : game.users) {
                if(topusers.containsKey(user.points)) {
                    String drawusers = topusers.get(user.points);
                    drawusers = drawusers + ", " + user.nick;
                    topusers.put(user.points, drawusers);
                }
                else {
                    topusers.put(user.points, user.nick);
                }
            }
            if(amount == defaultamount) {
                sendMessageWrapper(channel, sender, "Top " + amount + " users in this game:");
            }
            else {
                sendMessageWrapper(channel, sender, "A PM has been sent to you with the top " + amount + " users in this game.");
            }
            
            Integer i = 1;
            Integer key = topusers.lastKey();
            String topusersStr = new String();
            while(i <= amount) {
                if(amount == defaultamount) {
                    topusersStr = topusersStr + i + ". " + topusers.get(key) + " with " + key + " points. | ";
                }
                else {
                    sendMessageWrapper(sender, null, i + ". " + topusers.get(key) + " with " + key + " points.");
                }
                key = topusers.lowerKey(key);
                if(key == null) {
                    break;
                }
                i++;
            }

            if(amount == defaultamount) {
                sendMessageWrapper(channel, null, topusersStr.substring(0, topusersStr.length()-3));
            }
        }
        else {
            sendMessageWrapper(channel, sender, MSG_NOGAME);
        }
    }
    
    public void WGPwd(String sender, String login, String hostname, Command command) {
        if(command.arguments.length == 2) {
            User user;
            for(Game game : games.values()) {
                user = getUser(game, sender, login, hostname);
                if(user != null) {
                    user.setPassword(command.arguments[1]);
                    if(game.autosave) {
                        saveGame(game);
                    }
                }
            }
            sendMessage(sender, "Password set for all users matching your connection details.");
            sendMessage(sender, "Use !wglogin <nick> <password> to login if any of your connection details change.");
            sendMessage(sender, "If you change your nick, use your old nick in the !wglogin command.");
        }
        else {
            sendMessage(sender, "WGPWD usage: !wgpwd <newpassword>");
        }
    }
    
    public void WGLogin(String sender, String login, String hostname, Command command) {
        if(command.arguments.length == 3) {
            User user;
            boolean exists = false;
            boolean setpassword = false;
            boolean loggedin = false;
            for(Game game : games.values()) {
                user = getUserByNick(game, command.arguments[1]);
                if(user != null) {
                    exists = true;
                    if(user.passwordhash != null) {
                        setpassword = true;
                        if(user.checkPassword(command.arguments[2])) {
                            loggedin = true;
                            user.nick = sender;
                            user.login = login;
                            user.hostname = hostname;
                            if(game.autosave) {
                                saveGame(game);
                            }
                        }
                    }
                }
            }

            if(!exists) {
                sendMessageWrapper(sender, null, "The given nick does not match an account");
                return;
            }

            if(!setpassword) {
                sendMessageWrapper(sender, null, "You have not set a password! Set a password using !wgpwd with your old connection.");
                sendMessageWrapper(sender, null, "If you are not capable of setting a password, contact an admin.");
                return;
            }

            if(!loggedin) {
                sendMessageWrapper(sender, null, "Wrong password!");
                return;
            }

            sendMessageWrapper(sender, null, "Logged in succefully, your connection details have been changed.");
        }
        else {
            sendMessageWrapper(sender, null, "WGLOGIN usage: !wglogin <nick> <password>");
        }
    }
    
    // Admin commands
    
    public void WGAdmin(String sender, String login, String hostname, Command command) {
        if(adminpass == null) {
            System.out.println("[!] Admin identification attempt (no pass set). Nick: " + sender + " Login: " + login + " Hostname: " + hostname);
            sendMessage(sender, "No admin password has been specified for this server.");
            sendMessage(sender, "Please connect to the server that the bot connected to first," +
                    " and use the password that was given when the program started.");
            sendMessage(sender, "Also make sure you haven't identified yourself as an admin yet, this can only happen once.");
            sendMessage(sender, "This identification attempt will be logged.");
        }
        else {
            if(command.arguments.length == 2) {
                if(command.arguments[1].equals(adminpass)) {
                    admindetails.put("nick", sender);
                    admindetails.put("login", login);
                    admindetails.put("hostname", hostname);
                    adminpass = null;
                    sendMessage(sender, "You have succefully identified yourself as the admin.");
                }
                else {
                    System.out.println("[!] Wrong admin identification attempt! Nick: " + sender + " Login: " + login + " Hostname: " + hostname);
                    sendMessage(sender, "Wrong password. This attempt will be logged.");
                }
            }
            else {
                sendMessage(sender, "WGADMIN usage: !wgadmin <password>");
            }
        }
    }
    
    public void WGJoin(String sender, Command command) {
        if(command.arguments.length == 2) {
            joinChannel(command.arguments[1]);
        }
        else if(command.arguments.length == 3) {
            joinChannel(command.arguments[1], command.arguments[2]);
        }
        else {
            sendMessage(sender, "WGJOIN usage: !wgjoin <#channel> [password]");
        }
    }
    
    public void WGNewGame(String sender, Command command) {
        // This function will create an empty game with no users, no words, and 1 channel.
        if(command.arguments.length == 2) {
            // The game will be put in the application-wide HashMap games
            // in the following way: <"servername channel", gameobject>
            games.put(getServer() + " " + command.arguments[1], new Game());
        }
        else {
            sendMessage(sender, "WGNEWGAME usage: !wgnewgame <#initialchannel>");
        }
    }
    
    public void WGListGames(String sender) {
        if(games.isEmpty()) {
            sendMessage(sender, "There are no games running.");
        }
        else {
            for(String servchan : games.keySet()) {
                sendMessage(sender, servchan + " " + games.get(servchan).id);
            }
        }
    }
    
    public void WGGiveWord(String sender, Command command) {
        if((command.arguments.length == 3) || (command.arguments.length == 4)) {
            Game game = getGame(command.arguments[1]);
            if(game != null) {              
                User user = getUserByNick(game, command.arguments[2]);
                if(user != null) {
                    Integer amount = 1;
                    if(command.arguments.length == 4) {
                        amount = Integer.parseInt(command.arguments[3]);
                    }
                    Integer left = amount;
                    while(left > 0) {
                        user.addUnsetWord();
                        left--;
                    }
                    sendMessage(sender, amount + " word(s) given to " + user);
                }
                else {
                    sendMessage(sender, MSG_NOSUCHUSER);
                }
            }
            else {
                sendMessage(sender, MSG_NOSUCHGAME);
            }
        }
        else {
            sendMessage(sender, "WGGIVEWORD usage: !wggiveword <gameid> <user> [amount]");
            sendMessage(sender, "A negative amount is allowed.");
            sendMessage(sender, "Use !wglistgames to find the gameid.");
        }
    }
    
    public void WGSave(String sender, Command command) {
        if(command.arguments.length == 2) {
            Game game = getGame(command.arguments[1]);
            if(saveGame(game)) {
                sendMessage(sender, "Game saved.");
            }
            else {
                sendMessage(sender, MSG_SAVEERROR);
            }
        }
        else {
            sendMessage(sender, "WGSAVE usage: !wgsave <gameid>");
            sendMessage(sender, "Use !wglistgames to find the gameid.");
        }
    }
    
    public void WGLoad(String sender, Command command) {
        if(command.arguments.length == 3) {
            try {
                FileInputStream fis = new FileInputStream(savefolder + command.arguments[2]);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Game game = (Game)ois.readObject();
                ois.close();
                
                games.put(getServer() + " " + command.arguments[1], game);
                
                sendMessage(sender, "Game loaded.");
            }
            catch(Exception ex) {
                sendMessage(sender, "Error occured while loading. See stacktrace in STDERR. Did you type the gameid correctly?");
            }
        }
        else {
            sendMessage(sender, "WGLOAD usage: !wgload <#channel> <gameid>");
            sendMessage(sender, "You need to know the gameid which was used previously to save the game.");
        }
    }
    
    public void WGSafeQuit(String sender) {
        for(Game game : new HashSet<Game>(games.values())) {
            if(saveGame(game)) {
                sendMessage(sender, "Game " + game.id + " saved.");
            }
            else {
                sendMessage(sender, MSG_SAVEERROR);
            }
        }
        
        // Wait a second or 2, otherwise the bot will quit before the queue is processed
        try {
            Thread.sleep(2000);
        } catch (Exception e2) {
            // Do nothing.
        }
        
        disconnect();
        System.out.println("Exiting, WGSAFEQUIT command issued.");
        System.exit(0);
    }

    public void WGResetWord(String sender, Command command) {
        if(command.arguments.length == 4) {
            Game game = getGame(command.arguments[1]);
            if(game != null) {  
                User user = getUserByNick(game, command.arguments[2]);
                if(user != null) {
                    for(Word word : user.wordobjs) {
                        if(word.word.equals(command.arguments[3])) {
                            user.wordobjs.remove(word);
                            user.addUnsetWord();
                            sendMessage(sender, MSG_WORDRESET);
                            return;
                        }
                    }
                    sendMessage(sender, MSG_NOSUCHWORD);
                }
                else {
                    sendMessage(sender, MSG_NOSUCHUSER);
                }
            }
            else {
                sendMessage(sender, MSG_NOSUCHGAME);
            }
        }
        else {
            sendMessage(sender, "WGRESETWORD usage: !wgresetword <gameid> <user> <word>");
        }
    }

    public void WGAutoSave(String sender, Command command) {
        if(command.arguments.length == 2) {
            Game game = getGame(command.arguments[1]);
            if(game != null) {
                game.autosave = (game.autosave == false);  // Toggle the autosave bool
                sendMessage(sender, "Autosaving of game " + game.id + " set to " + game.autosave.toString());
            }
            else {
                sendMessage(sender, MSG_NOSUCHGAME);
            }
        }
        else {
            sendMessage(sender, "WGAUTOSAVE usage: !wgautosave <gameid>");
            sendMessage(sender, "Use !wglistgames to find the gameid.");
        }
    }

    public void WGOverridePwd(String sender, Command command) {
        if(command.arguments.length == 3) {
            User user;
            for(Game game : games.values()) {
                user = getUserByNick(game, command.arguments[1]);
                if(user != null) {
                    user.setPassword(command.arguments[2]);
                    if(game.autosave) {
                        saveGame(game);
                    }
                }
            }
            sendMessageWrapper(sender, null, "Password set for all users matching this nick.");
        }
        else {
            sendMessageWrapper(sender, null, "WGOVERRIDEPWD usage: !wgoverridepwd <user> <newpassword>");
        }
    }

    public void WGResetAllPoints(String sender, Command command) {
        if(command.arguments.length == 2) {
            Game game = getGame(command.arguments[1]);
            if(game != null) {  
                for(User user : game.users) {
                    user.points = 0;
                }
                sendMessageWrapper(sender, null, "All points have been reset to 0.");
            }
            else {
                sendMessageWrapper(sender, null, MSG_NOSUCHGAME);
            }
        }
        else {
            sendMessageWrapper(sender, null, "WGRESETALLPOINTS usage: !wgresetallpoints <game>");
        }
    }
}
