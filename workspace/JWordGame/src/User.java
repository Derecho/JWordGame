import java.util.HashMap;


public class User {
	
	String nick, login, hostname;
	Integer points, wordsLeft;
	HashMap<String, Integer> words;

	public User(String nick, String login, String hostname) {
		this.nick = nick;
		this.login = login;
		this.hostname = hostname;
		
		points = 0;
		wordsLeft = 0;
		words = new HashMap<String, Integer>();
	}
	
	public boolean setWord(String word) {
		if(wordsLeft > 0) {
			words.put(word, 0);
			wordsLeft--;
			return true;
		}
		else {
			return false;
		}
	}
	
	public String listWords() {
		String returnstr = new String();
		
		if(words.isEmpty()) {
			return "You haven't set any words.";
		}
		
		for(String word : words.keySet()) {
			returnstr = returnstr + word + " (" + words.get(word) + ") ";
		}
		
		return returnstr;
	}
	
}
