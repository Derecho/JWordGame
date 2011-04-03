
public enum Commands {
	
	// All enum names
	WG, WGHELP, WGSIGNUP, WGPOINTS, WGSET, WGLISTWORDS, WGADMIN, WGADMINHELP, WGJOIN, WGNEWGAME, WGLISTGAMES, WGGIVEWORD, UNKNOWN, NOCOMMAND;

	public static Command toCommand(String str) {
		// Use this function to get the enum value for a string
		
		String [] commandList;
		commandList = str.split(" ");
		
		if("!".equals(commandList[0].substring(0,1))) {
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
