import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;


public class User implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String nick, login, hostname, defaultchannel;
	Integer points, wordsLeft;
	Set<Word> wordobjs;
	byte[] passwordhash;

	public User(String nick, String login, String hostname) {
		this.nick = nick;
		this.login = login;
		this.hostname = hostname;
		
		defaultchannel = null;
		passwordhash = null;
		points = 0;
		wordsLeft = 0;
		wordobjs = new HashSet<Word>();
	}
	
	public boolean setWord(String word) {
		if(wordsLeft > 0) {
			wordobjs.add(new Word(word));
			wordsLeft--;
			return true;
		}
		else {
			return false;
		}
	}
	
	public String listWords() {
		String returnstr = new String();
		
		if(wordobjs.isEmpty()) {
			return "You haven't set any words.";
		}
		
		for(Word word : wordobjs) {
			returnstr = returnstr + word + " (" + word.mentions + "), ";
		}
		
		return returnstr.substring(0, returnstr.length()-2);
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
