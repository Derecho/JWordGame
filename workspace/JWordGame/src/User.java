import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


public class User implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    String nick, login, hostname, defaultchannel;
    Integer points;
    LinkedList<UnsetWord> unsetwordobjs;
    Set<Word> wordobjs;
    byte[] passwordhash;

    public User(String nick, String login, String hostname) {
        this.nick = nick;
        this.login = login;
        this.hostname = hostname;
        
        defaultchannel = null;
        passwordhash = null;
        points = 0;
        unsetwordobjs = new LinkedList<UnsetWord>();
        wordobjs = new HashSet<Word>();
    }
    
    public String toString() {
        return nick;
    }
    
    public void addUnsetWord() {
        unsetwordobjs.add(new UnsetWord());
    }

    public void removeUnsetWord() {
        unsetwordobjs.removeFirst();
    }

    public boolean hasUnsetWords() {
        return !unsetwordobjs.isEmpty();
    }

    public boolean setWord(String word) {
        if(hasUnsetWords()) {
            wordobjs.add(new Word(word));
            removeUnsetWord();
            return true;
        }
        else {
            return false;
        }
    }
    
    public String listWords() {
        String returnstr = new String();
        
        if(wordobjs.isEmpty()) {
            returnstr = returnstr + "You haven't set any words. ";
        }
        
        for(Word word : wordobjs) {
            returnstr = returnstr + word + " (" + word.mentions + "), ";
        }
        returnstr = returnstr.substring(0, returnstr.length()-2) + ". ";
        
        if(hasUnsetWords()) {
            returnstr = returnstr + "You have " + unsetwordobjs.size() + " word(s) left to set.";
            return returnstr;
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
            return Arrays.equals(md.digest(), passwordhash);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
}
