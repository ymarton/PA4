package IC.Parser;

public class LexicalError extends Exception {
	
	 public LexicalError(String message) {
	     super(message);
	    }
	 
    public LexicalError(int line, int column, String message) {
        System.err.println(line + ":" + column + " : lexical error; " + message);
    }
}
