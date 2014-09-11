import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

// This is an actual game. A game could potentially run on a few channels or even servers.
// More typical behaviour would be to give each channel it's own game though.

public class Game implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Set<User> users;
    Integer pointsreference, maxwords;
    String id, commandprefix;
    Boolean autosave;
    
    public Game() {
        id = Integer.toHexString(System.identityHashCode(this));
        users = new HashSet<User>();
        pointsreference = 100; // Amount of total points rewarded after a week
        maxwords = 2; // Maximum amount of words that can be set by a user.
        commandprefix = "!";
        autosave = false;
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
    
    public User getUserByNick(String nick) {
        // NOT to be used normally! Only to be used by admins who are sure of the nick.
        for(User user : users) {
            if(user.nick.equals(nick)) {
                return user;
            }
        }
        return null;
    }
    
}
