package IC.Semantic;

public class SemanticError extends Exception
{
	int line;
	
	/**
	 * @param message - desired error message
	 * @param line - on which line of the parsed file the error occurred
	 */
	public SemanticError(String message, int line)
	{
		super("semantic error at line " + String.valueOf(line) +": " + message);
		this.line = line;
	}
}
