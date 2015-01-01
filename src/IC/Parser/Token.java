package IC.Parser;

import java_cup.runtime.Symbol;

public class Token extends Symbol {
	
	private int tag, line, column;
	private String tagAsString, value;

	public Token(int tag, int line, int column, String tagAsString, String value) {
		super(tag, line, -1, value);
		this.line = line;
		this.column = column;
		this.tag = tag;
		this.value = value;
		this.tagAsString = tagAsString;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
	
	public int getTag() {
		return tag;
	}
	
	public String getTagAsString() {
		return tagAsString;
	}
	
	public String getValue() {
		return value;
	}
	
	public static void PrintHeader() {
		System.out.println("token\ttag\tline :column");
	}
	
	public static void PrintToken(String token, String tagAsString, int line ,int column) {
		System.out.println(token+"\t"+tagAsString+"\t"+line+":"+column);
	}
	
	public static void PrintTokenError(String token, int line, int column) {
		System.err.println("Error!\t"+token+"\t"+"\t"+line+":"+column);
	}
}
