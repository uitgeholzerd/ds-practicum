package be.uantwerpen.ds.ns;

/**
 * Attempt at a communication protocol for nodes and/or server
 * First parameter is the command, second specifies its parameters in printf format
 *
 */
public enum Protocol {
	REGISTER ("REGISTER", "%s"),
	REG_ACK ("REG_ACK", "%d"),
	PREVNODE ("PREVNODE", "%d"),
	NEXTNODE ("NEXTNODE", "%d"),
	LEAVE ("LEAVE", "%s");
	
	private final String command;
	private final String parameters;
	Protocol(String command, String parameters){
		this.command = command;
		this.parameters = parameters;
	}
	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 * 
	 * This returns a string that can be used with printf or String.format
	 */
	public String toString(){
		return command + " " + parameters;
		
	}
	/**
	 * Get the keyword for this command
	 * @return The keyword
	 */
	public String getCommand(){
		return this.command;
	}
}
