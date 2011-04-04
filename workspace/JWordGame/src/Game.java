import java.util.HashSet;
import java.util.Set;

// This is an actual game. A game could potentially run on a few channels or even servers.
// More typical behaviour would be to give each channel it's own game though.

public class Game {
	
	Set<User> users;
	Integer maxpoints, maxwords;
	String commandprefix;
	
	public Game() {
		users = new HashSet<User>();
		maxpoints = 10; // Maximum amount of points that is rewarded to a guesser.
		maxwords = 2; // Maximum amount of words that can be set by a user.
		commandprefix = "!";
	}
	
	public boolean addUser(User newuser) {
		for(User user : users) {
			if(user.nick.equals(newuser.nick)) {
				return false;
			}
		}
		
		// We havent returned false yet, so the user must not exist.
		users.add(newuser);
		return true;
	}
	
	public User getUser(String nick, String login, String hostname) {
		for(User user : users) {
			if(user.nick.equals(nick) && user.login.equals(login) && user.hostname.equals(hostname)) {
				return user;
			}
		}
		return null;
	}
	
	public Integer calcReward(Integer mentions) {
		// Returns the amount of points to be rewarded to the guesser of the word.
		// This depends on the amount of mentions the original user had.
		// This should be changable somehow.
		return (int)(maxpoints / (mentions/1.5 + 1));
	}
	
}
