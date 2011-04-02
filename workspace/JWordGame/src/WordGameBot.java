import org.jibble.pircbot.*;


public class WordGameBot extends PircBot {
	
	public WordGameBot() {
		this.setName("JWG");
	}
	
	public void onMessage(String channel, String sender,
			String login, String hostname, String message) {
		if ("!wg".equals(message)) {
			sendMessage(channel, sender + ": This is JWordGame, an irc game based on (accidentally) guessing words set by others. Type !wg-help for more help.");
		}
	}
}