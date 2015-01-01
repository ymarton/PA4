package IC.Parser;

public class SyntaxError extends Exception {
    public SyntaxError(int line, int column, String expected, String found) {
        System.err.println(line + ":" + column + " : syntax error; expected " + expected + ", but found '" + found + "'.");
    }
    
    public SyntaxError(int line, int column, String msg) {
        System.err.println(line + ":" + column + " : syntax error; " + msg);
    }
}
