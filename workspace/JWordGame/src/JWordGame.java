import java.util.HashMap;
import java.util.Random;

import org.jibble.pircbot.*;

public class JWordGame {

    static HashMap<String, Game> games;
    static String adminpass;
    
    public static void main(String[] args) throws Exception {
        // First create the games HashMap
        games = new HashMap<String, Game>();
        
        // TODO Load saved game objects and add them to the hashmap.
        
        // Generate a random password for the admin to identify itself with
        adminpass = generatePassword(10);
        System.out.println("[!] ADMIN PASSWORD: " + adminpass);
        
        // Launch the first bot (a bot can be connected to 1 server and multiple channels)
        WordGameBot bot = new WordGameBot(games);
        bot.adminpass = adminpass;
        
        // Enable debugging output
        //bot.setVerbose(true);
        
        // Connect to the IRC server
        bot.connect("irc.sector5d.org", 6697, new TrustingSSLSocketFactory());

    }
    
    public static String generatePassword(Integer length) {
        // Generates a random password of given length
        Random generator = new Random();
        char[] pass = new char[length];
        
        while(length > 0) {
            pass[length-1] = (char)(generator.nextInt(93) + 33);
            length--;
        }
        
        return new String(pass);
    }

}
