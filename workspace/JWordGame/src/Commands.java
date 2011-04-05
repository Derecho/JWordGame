
public enum Commands {
	
	// All enum names
	WG, WGHELP, WGSIGNUP, WGPOINTS, WGSTATUS, WGSET, WGLISTWORDS, WGDONATE, WGDEFAULTCHANNEL, WGTOP, WGPWD, WGLOGIN, WGADMIN, WGADMINHELP, WGJOIN, WGNEWGAME, WGLISTGAMES, WGGIVEWORD, WGSAVE, WGLOAD, WGSAFEQUIT, UNKNOWN, NOCOMMAND;

	public static Command toCommand(String str, String prefix) {
		// Use this function to get the enum value for a string
		
		String command;
		String [] commandList;
		commandList = str.split(" ");
		command = commandList[0].toUpperCase();
		
		if(prefix == null) {  // Prefix is null, which means the command was sent in a PM.
			command = command.replaceAll("[^A-Z]", ""); // Remove any prefix the use may have used.
			// Add WG in the beginning of the command if it was left out
			if(!"WG".equals(command.substring(0,2))) {
				command = "WG" + command;
			}
			
			try {
				return new Command(valueOf(command), commandList);  // The additional replaceAll here gets rid of any potential prefixes the user may have used.
			}
			catch (Exception ex) {
				return new Command(UNKNOWN, null);
			}
		}
		else {
			if(command.substring(0,1).equals(prefix)) {
				try {
					return new Command(valueOf(command.substring(1)), commandList);
				}
				catch (Exception ex) {
					return new Command(UNKNOWN, null);
				}
			}
			else {
				return new Command(NOCOMMAND, null);
			}			
		}
	}
}
