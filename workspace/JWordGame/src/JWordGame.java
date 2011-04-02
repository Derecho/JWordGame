import org.jibble.pircbot.*;

public class JWordGame {

	public static void main(String[] args) throws Exception {
		// First create the bot
		WordGameBot bot = new WordGameBot();
		
		// Enable debugging output
		bot.setVerbose(true);
		
		// Connect to the IRC server and join the channel
		bot.connect("irc.sector5d.org", 6697, new TrustingSSLSocketFactory());
		bot.joinChannel("#ubersoft");

	}

}