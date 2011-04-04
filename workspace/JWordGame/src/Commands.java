
public enum Commands {
	
	// All enum names
	WG, WGHELP, WGSIGNUP, WGPOINTS, WGSTATUS, WGSET, WGLISTWORDS, WGADMIN, WGADMINHELP, WGJOIN, WGNEWGAME, WGLISTGAMES, WGGIVEWORD, WGSAVE, WGLOAD, UNKNOWN, NOCOMMAND;

	public static Command toCommand(String str, String prefix) {
		// Use this function to get the enum value for a string
		
		String [] commandList;
		commandList = str.split(" ");
		
		if(prefix == null) {  // Prefix is null, which means the command was sent in a PM.
			try {
				return new Command(valueOf(commandList[0].toUpperCase().replaceAll("[^A-Z]", "")), commandList);  // The additional replaceAll here gets rid of any potential prefixes the user may have used.
			}
			catch (Exception ex) {
				return new Command(UNKNOWN, null);
			}
		}
		else {
			if(commandList[0].substring(0,1).equals(prefix)) {
				try {
					return new Command(valueOf(commandList[0].substring(1).toUpperCase()), commandList);
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
