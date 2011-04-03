// Container object for commands.

public class Command {
	Commands command;
	String[] arguments;
	
	public Command(Commands command, String[] arguments) {
		this.command = command;
		this.arguments = arguments;
	}

}
