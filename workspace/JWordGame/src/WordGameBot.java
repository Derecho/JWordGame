import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

import org.jibble.pircbot.*;


public class WordGameBot extends PircBot {
	HashMap<String, Game> games;
	HashMap<String, String> admindetails;
	String adminpass, savefolder;
	
	final String MSG_NOTREGISTERED = "You do not have an account yet or have not signed in properly. Use !wgsignup to sign up for an account.";
	final String MSG_MAXWORDSREACHED = "Uh oh! You seem to have reached the maximum amount of unset words. Your unset word will now be randomly be given to someone.";
	final String MSG_WGINFO = "This is JWordGame, an irc game based on (accidentally) guessing words set by others. Type !wghelp for more help.";
	final String MSG_NOGAME = "There is no game in progress on this channel (yet).";
	final String MSG_WGHELP1 = "Type !wgsignup to signup for the wordgame.";
	final String MSG_WGHELP2 = "Other available commands: !wgpoints !wgstatus !wgdonate !wgdefaultchannel, and PM-only: !wgset !wglistwords";
	final String MSG_SIGNUPSUCCESS = "You have succefully signed up for the wordgame.";
	final String MSG_SIGNUPFAIL = "A user with that nickname already exists.";
	final String MSG_WANTDEFAULTCHANNEL = "If you want to use a default channel, type !wgdefaultchannel in the channel you would like to use as your default channel.";
	final String MSG_WORDSET = "Word set.";
	final String MSG_NOUNSETWORDS = "You do not have any unset words.";
	final String MSG_DONATED = "Word donated.";
	final String MSG_DEFAULTCHANNEL = "Your default channel has been set to: ";
	final String MSG_INVALIDNUMBER = "Please provide a valid number.";
	
	public WordGameBot(HashMap<String, Game> games) {
		this.games = games;
		admindetails = new HashMap<String, String>();
		adminpass = null;
		savefolder = "saves/";
		
		setName("JWG");
		setLogin("JWordGame");
		setVersion("JWordGame BETA by Derecho.");
		setMessageDelay(0);
		sendRawLineViaQueue("MODE " + getNick() + " +B");  // A Sector5d.org thing.
	}
	
	// Inherited methods
	
	public void onMessage(String channel, String sender,
			String login, String hostname, String message) {
		// A message has been received, parse it.
		
		// Get the game if there is one running
		Game game = games.get(getServer() + " " + channel);
		
		// Get the user if (s)he is registered
		User user = getUser(game, sender, login, hostname);
		
		// First try all the commands
		
		Command command;
		if(game != null) {
			command = Commands.toCommand(message, game.commandprefix);
		}
		else {
			command = Commands.toCommand(message, "!");
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
			if(user != null) { WGPoints(channel, user);	}
			else { tellNotRegistered(channel, sender);	}
			break;
		case WGSTATUS:
			WGStatus(channel, sender);
			break;
		case WGDONATE:
			if(user != null) { WGDonate(channel, user, game, command);	}
			else { tellNotRegistered(channel, sender);	}
			break;
		case WGDEFAULTCHANNEL:
			if(user != null) { WGDefaultChannel(channel, user, command);	}
			else { tellNotRegistered(channel, sender);	}
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
		Command command;
		command = Commands.toCommand(message, null);
		
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
		case WGADMIN:
			WGAdmin(sender, login, hostname, command);
			break;
		// TODO add a WGLOGIN command, for when someone's nickname, loginname or hostname has changes.
		// This would probably work with a password.
		}
		
		if(isAdmin(sender, login, hostname)) {
			switch(command.command) {
			case WGADMINHELP:
				sendMessage(sender, "Available commands: !wgjoin, !wgnewgame, !wglistgames, !wggiveword !wgsave !wgload");
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
			}
		}
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
			if(gameid.equals(Integer.toHexString(System.identityHashCode(games.get(servchan))))) {
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
	
	public void sendMessageWrapper(String recipient, String nick, String message) {
		if(nick == null) {
			sendMessage(recipient, message);
		}
		else {
			sendMessage(recipient, nick + ": " + message);
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
				for(String word : setter.words.keySet()) {
					if(message.contains(word)) {
						if(guesser.equals(setter)) {
							// User mentioned his own word
							guesser.words.put(word, guesser.words.get(word) + 1);
						}
						else {
							// User mentioned someone elses word
							// First calculate the rewards
							Integer guesserreward = game.calcGuesserReward(setter.words.get(word));
							Integer setterreward = game.calcSetterReward(setter.words.get(word));
							
							// Inform the guesser about his accomplishment							
							sendMessage(channel, guesser.nick + ": Congratiulations! You have guessed the word '" + word + "' set by " + setter.nick + "!");
							sendMessage(channel, "Rewards: " + guesser.nick + " " + guesserreward + " points, " + setter.nick + " " + setterreward + " points.");
							
							// Give the guesser and setter a reward
							guesser.points += guesserreward;
							setter.points += setterreward;
							
							// Remove the word from the setter
							setter.words.remove(word);
							
							// Assign word to guesser or random person
							if(guesser.wordsLeft <= game.maxwords) {
								guesser.wordsLeft++;
							}
							else {
								sendMessageWrapper(channel, guesser.nick, MSG_MAXWORDSREACHED);
								
								// TODO Make a much more efficient method for the below code.
								Random generator = new Random();
								Integer randomint = generator.nextInt();
								Integer i = 0;
								for(User randomuser : game.users) {
									if(i == randomint) {
										randomuser.wordsLeft++;
										break;
									}
								}
							}
						}
					}
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
		}
		else {
			sendMessageWrapper(channel, sender, MSG_SIGNUPFAIL);
		}
	}
	
	public void WGPoints(String channel, User user) {
		sendMessage(channel, user.nick + ": You have " + user.points + " points.");
	}
	
	public void WGStatus(String channel, String sender) {
		Integer setwords = 0;
		String availablewords = new String();
		
		Game game;
		game = games.get(getServer() + " " + channel);
		
		if(game == null) {
			sendMessageWrapper(channel, sender, MSG_NOGAME);
			return;
		}
		
		// TODO Sort the users based on words left (descending)
		for(User user : game.users) {
			setwords += user.words.size();
			if(user.wordsLeft > 0) {
				availablewords += user.nick + " (" + user.wordsLeft + ") ";
			}
		}
		
		sendMessage(channel, sender + ": " + setwords + " words have been set.");
		if(!"".equals(availablewords)) {
			sendMessage(channel, sender + ": The following users can set words: " + availablewords);
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
		if(user.wordsLeft > 0) {
			if(command.arguments.length == 2) {
				User recipient = getUserByNick(game, command.arguments[1]);
				user.wordsLeft--;
				recipient.wordsLeft++;
				sendMessage(channel, MSG_DONATED);
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
		Integer amount = 3;  // Default is to show the top 3 players
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
				topusers.put(user.points, user.nick);
			}
			sendMessageWrapper(channel, sender, "Top " + amount + " users in this game:");
			
			Integer i = 1;
			Integer key = topusers.lastKey();
			while(i <= amount) {
				sendMessageWrapper(channel, null, i + ". " + topusers.get(key) + " with " + key + " points.");
				key = topusers.lowerKey(key);
				if(key == null) {
					break;
				}
				i++;
			}
		}
		else {
			sendMessageWrapper(channel, sender, MSG_NOGAME);
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
		// TODO if no games, say that.
		for(String servchan : games.keySet()) {
			sendMessage(sender, servchan + " " + Integer.toHexString(System.identityHashCode(games.get(servchan))));
		}
	}
	
	public void WGGiveWord(String sender, Command command) {
		if(command.arguments.length == 3) {
			Game game = getGame(command.arguments[1]);
			if(game != null) {				
				User user = getUserByNick(game, command.arguments[2]);
				if(user != null) {
					user.wordsLeft++;
					sendMessage(sender, "1 Word given to " + user.nick);
				}
				else {
					sendMessage(sender, "No such user could be found.");
				}
			}
			else {
				sendMessage(sender, "No such game could be found.");
			}
		}
		else {
			sendMessage(sender, "WGGIVEWORD usage: !wggiveword <gameid> <user>");
			sendMessage(sender, "Use !wglistgames to find the gameid.");
		}
	}
	
	public void WGSave(String sender, Command command) {
		if(command.arguments.length == 2) {
			Game game = getGame(command.arguments[1]);
			if(game != null) {
				try {
					// If the directory does not exist yet, create it
					if(!new File(savefolder).exists()) {
						new File(savefolder).mkdirs();
					}
					
					FileOutputStream fos = new FileOutputStream(savefolder + command.arguments[1]);
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(game);
					oos.close();
					
					sendMessage(sender, "Game saved.");
				}
				catch(Exception ex) {
					sendMessage(sender, "Error occured while saving. See stacktrace in STDERR.");
					ex.printStackTrace();
				}
			}
			else {
				sendMessage(sender, "No such game.");
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
			try {
				// If the directory does not exist yet, create it
				if(!new File(savefolder).exists()) {
					new File(savefolder).mkdirs();
				}
				
				FileOutputStream fos = new FileOutputStream(savefolder + Integer.toHexString(System.identityHashCode(game)));
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(game);
				oos.close();
			}
			catch(Exception ex) {
				sendMessage(sender, "Error occured while saving. See stacktrace in STDERR.");
				ex.printStackTrace();
			}
		}
		sendMessage(sender, "Games saved.");
		disconnect();
		System.out.println("Exiting, WGSAFEQUIT command issued.");
		System.exit(0);
	}
}