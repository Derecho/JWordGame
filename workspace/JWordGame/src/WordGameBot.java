import java.util.HashMap;

import org.jibble.pircbot.*;


public class WordGameBot extends PircBot {
	HashMap<String, Game> games;
	HashMap<String, String> admindetails;
	String adminpass;
	
	public WordGameBot(HashMap<String, Game> games) {
		this.games = games;
		admindetails = new HashMap<String, String>();
		adminpass = null;
		
		setName("JWG");
	}
	
	// Inherited methods
	
	public void onMessage(String channel, String sender,
			String login, String hostname, String message) {
		// A message has been received, parse it.
		
		// Get the user if (s)he is registered
		User user = getUser(channel, sender, login, hostname);
		
		// First try all the commands
		Command command;
		command = Commands.toCommand(message);
		
		switch(command.command) {
		case WG:
			sendMessage(channel, sender + ": This is JWordGame, an irc game based on " +
					"(accidentally) guessing words set by others. Type !wghelp for more help.");
			break;
		case WGHELP:
			sendMessage(channel, sender + ": Type !wgsignup to signup for the wordgame.");
			sendMessage(channel, sender + ": Other available commands: !wgpoints !wgstatus, and PM-only: !wgset !wglistwords");
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
		case WGSET:
		case WGLISTWORDS:
			sendMessage(channel, sender + ": You can only do this in a PM.");
			break;
		}
	}
	
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		Command command;
		command = Commands.toCommand(message);
		
		switch(command.command) {
		case WGSET:
			WGSet(sender, login, hostname, command);
			break;
		case WGLISTWORDS:
			WGListWords(sender, login, hostname, command);
			break;
		case WGADMIN:
			WGAdmin(sender, login, hostname, command);
			break;		
		}
		
		if(isAdmin(sender, login, hostname)) {
			switch(command.command) {
			case WGADMINHELP:
				sendMessage(sender, "Available commands: !wgjoin, !wgnewgame, !wglistgames, !wggiveword");
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
			}
		}
	}
	
	// Own methods
	
	public User getUser(String channel, String sender, String login, String hostname) {
		Game game;
		game = games.get(getServer() + " " + channel);
		
		if(game == null) {
			return null;
		}
		
		User user;
		user = game.getUser(sender, login, hostname);
		
		return user;
	}
	
	public void tellNotRegistered(String channel, String sender) {
		sendMessage(channel, sender + ": You do not have an account yet or have not signed in properly." +
				" Use !wgsignup to sign up for an account.");
	}
	
	public void tellNotRegisteredPM(String sender) {
		sendMessage(sender, "You do not seem to have an account on that channel yet, or you have not signed in properly." +
				" Use !wgsignup in the channel to sign up for an account");
	}
	
	// User commands
	
	public void WGSignup(String channel, String sender, String login, String hostname) {		
		Game game;
		game = games.get(getServer() + " " + channel);
		
		if(game == null) {
			sendMessage(channel, sender + ": There is no game in progress on this channel (yet).");
			return;
		}
		
		if(game.addUser(new User(sender, login, hostname))) {
			sendMessage(channel, sender + ": You have succefully signed up for the wordgame.");
		}
		else {
			sendMessage(channel, sender + ": A user with that nickname already exists.");
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
			sendMessage(channel, sender + ": There is no game in progress on this channel (yet).");
			return;
		}
		
		for(User user : game.users) {
			setwords += user.words.size();
			if(user.wordsLeft > 0) {
				availablewords += user.nick + " (" + user.wordsLeft + ") ";
			}
		}
		
		sendMessage(channel, sender + ": " + setwords + " words have been set. The following users can set words: " + availablewords);
	}
	
	public void WGSet(String sender, String login, String hostname, Command command) {
		if(command.arguments.length == 3) {
			User user;
			user = getUser(command.arguments[1], sender, login, hostname);
			if(user == null) {
				tellNotRegisteredPM(sender);
			}
			else {
				if(user.setWord(command.arguments[2])) {
					sendMessage(sender, "Word set.");
				}
				else {
					sendMessage(sender, "You cannot set any words.");
				}
			}
		}
		else {
			sendMessage(sender, "WGSET usage: !wgset <#channel> <word>");
		}
	}
	
	public void WGListWords(String sender, String login, String hostname, Command command) {
		if(command.arguments.length == 2) {
			User user;
			user = getUser(command.arguments[1], sender, login, hostname);
			if(user == null) {
				tellNotRegisteredPM(sender);
			}
			else {
				sendMessage(sender, user.listWords());
			}
		}
		else {
			sendMessage(sender, "WGLISTWORDS usage: !wglistwords <#channel>");
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
	
	public boolean isAdmin(String sender, String login, String hostname) {
		return (sender.equals(admindetails.get("nick")) && login.equals(admindetails.get("login")) && hostname.equals(admindetails.get("hostname")));
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
		for(String servchan : games.keySet()) {
			sendMessage(sender, servchan + " " + Integer.toHexString(System.identityHashCode(games.get(servchan))));
		}
	}
	
	public void WGGiveWord(String sender, Command command) {
		boolean gamefound, userfound;
		gamefound = false;
		userfound = false;
		
		// TODO It seems to me that the nested if and for loops below could be written somewhat cleaner.
		if(command.arguments.length == 3) {
			for(String servchan : games.keySet()) {
				if(command.arguments[1].equals(Integer.toHexString(System.identityHashCode(games.get(servchan))))) {
					// We found the right game, now let's find the user
					gamefound = true;
					for(User user : games.get(servchan).users) {
						if(command.arguments[2].equals(user.nick)) {
							// We found the right user
							userfound = true;
							user.wordsLeft++;
							sendMessage(sender, "1 Word given to " + user.nick + " on: " + servchan);
						}
					}
					if(!userfound) {
						sendMessage(sender, "No such user could be found.");
					}
				}
			}
			if(!gamefound) {
				sendMessage(sender, "No such game could be found.");
			}
		}
		else {
			sendMessage(sender, "WGGIVEWORD usage: !wggiveword <gameid> <user>");
			sendMessage(sender, "Use !wglistgames to find the gameid.");
		}
	}
}