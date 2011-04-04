import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;


public class User implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String nick, login, hostname;
	Integer points, wordsLeft;
	HashMap<String, Integer> words;
	byte[] passwordhash;

	public User(String nick, String login, String hostname) {
		this.nick = nick;
		this.login = login;
		this.hostname = hostname;
		
		passwordhash = null;
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
	
	public void setPassword(String password) {
		byte[] passbytes = password.getBytes();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(passbytes);
			passwordhash = md.digest();
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		
	}
	
	public boolean checkPassword(String password) {
		byte[] passbytes = password.getBytes();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(passbytes);
			return md.digest().equals(passwordhash);
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
}
