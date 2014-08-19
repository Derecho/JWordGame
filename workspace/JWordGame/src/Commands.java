
public enum Commands {
	
	// All enum names
	WG, WGHELP, WGSIGNUP, WGPOINTS, WGSTATUS, WGSET, WGLISTWORDS, WGDONATE, WGDEFAULTCHANNEL, WGTOP, WGPWD, WGLOGIN, WGADMIN, WGADMINHELP, WGJOIN, WGNEWGAME, WGLISTGAMES, WGGIVEWORD, WGSAVE, WGLOAD, WGSAFEQUIT, WGRESETWORD, WGAUTOSAVE, UNKNOWN, NOCOMMAND;

	public static Command toCommand(String str, String prefix, String botnick) {
		// Use this function to get the enum value for a string
		
		String command;
		String [] commandList;
		commandList = str.split(" ");
		command = commandList[0].toUpperCase();
		
		if(prefix == null) {  // Prefix is null, which means the command was sent in a PM.
			command = command.replaceAll("[^A-Z]", ""); // Remove any prefix the user may have used.
			// Add WG in the beginning of the command if it was left out
			if(!"WG".equals(command.substring(0,2))) {
				command = "WG" + command;
			}
			
			try {
				return new Command(valueOf(command), commandList);
			}
			catch (Exception ex) {
				return new Command(UNKNOWN, null);
			}
		}
		else {
			// Not a PM
			if(command.substring(0,1).equals(prefix)) {
				// User used a prefix
				try {
					return new Command(valueOf(command.substring(1)), commandList);
				}
				catch (Exception ex) {
					return new Command(UNKNOWN, null);
				}
			}
			else if(str.length() > botnick.length()) {  // preventing a StringIndexOutOfBoundsException here
				// TODO This looks a little hacky to me, maybe this could be done cleaner?
				if(str.substring(0, botnick.length()).equals(botnick)) {
					// Bot was highlighted
					command = str.substring(botnick.length()+1).trim(); // Get rid of the highlight
					command = command.replaceAll(prefix, "");  // Get rid of the prefix if it's given.
					commandList = command.split(" ");
					command = commandList[0].toUpperCase();
					
					if(!"WG".equals(command.substring(0, 2))) {  // Add WG if it wasn't in the command yet.
						command = "WG" + command;
					}
					
					try {
						return new Command(valueOf(command), commandList);
					}
					catch (Exception ex) {
						return new Command(UNKNOWN, null);
					}
				}
				else {
					return new Command(NOCOMMAND, null);
				}
			}
			else {
				return new Command(NOCOMMAND, null);
			}			
		}
	}
}
