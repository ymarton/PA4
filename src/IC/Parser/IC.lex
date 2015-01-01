package IC.Parser;

%%

%eofval{
	return new Token(sym.EOF, yyline+1, yycolumn+1, yytext(), yytext());
%eofval}

%{
	int getLineNumber() {
		return yyline+1;
	}

	int getColumnNumber() {
    	return yycolumn+1;
    }
%}

%class Lexer
%cup
%public
%function next_token
%type Token
%line
%column
%scanerror LexicalError

UPPERCASE = [A-Z]
LOWWERCASE = [a-z]
ALPHA = {UPPERCASE}|{LOWWERCASE}
DIGIT = [0-9]
UNDERSCORE = _
ALPHA_NUMERIC = {ALPHA}|{DIGIT}|{UNDERSCORE}
CLASS_ID = {UPPERCASE}({ALPHA_NUMERIC})*
ID = {ALPHA}({ALPHA_NUMERIC})*
INTEGER = ({DIGIT})+
NEWLINE = \n|\r|\r\n
WHITESPACE = [ ]|{NEWLINE}|\t
COMMENT_LINE = "//" ~{NEWLINE}
COMMENT_MULTILINE = "/*"
STRING = \"([\x20-\x21|\x23-\x5B|\x5D-\x7E]| \\t | \\n | \\\\ | \\\")*\"

%state COMMENT

%%

<YYINITIAL>{
	{WHITESPACE}	{}
	"class"		{return new Token(sym.CLASS, yyline+1, yycolumn+1, yytext(), yytext());}
	"extends"	{return new Token(sym.EXTENDS, yyline+1, yycolumn+1, yytext(), yytext());}
	"static"	{return new Token(sym.STATIC, yyline+1, yycolumn+1, yytext(), yytext());}
	"void"	{return new Token(sym.VOID, yyline+1, yycolumn+1, yytext(), yytext());}
	"int"	{return new Token(sym.INT, yyline+1, yycolumn+1, yytext(), yytext());}
	"boolean"	{return new Token(sym.BOOLEAN, yyline+1, yycolumn+1, yytext(), yytext());}
	"string"	{return new Token(sym.STRING_RESERVED, yyline+1, yycolumn+1, yytext(), yytext());}
	"return"	{return new Token(sym.RETURN, yyline+1, yycolumn+1, yytext(), yytext());}
	"if"	{return new Token(sym.IF, yyline+1, yycolumn+1, yytext(), yytext());}
	"else"	{return new Token(sym.ELSE, yyline+1, yycolumn+1, yytext(), yytext());}
	"while"	{return new Token(sym.WHILE, yyline+1, yycolumn+1, yytext(), yytext());}
	"break"	{return new Token(sym.BREAK, yyline+1, yycolumn+1, yytext(), yytext());}
	"continue"	{return new Token(sym.CONTINUE, yyline+1, yycolumn+1, yytext(), yytext());}
	"this"	{return new Token(sym.THIS, yyline+1, yycolumn+1, yytext(), yytext());}
	"new"	{return new Token(sym.NEW, yyline+1, yycolumn+1, yytext(), yytext());}
	"length"	{return new Token(sym.LENGTH, yyline+1, yycolumn+1, yytext(), yytext());}
	"true"	{return new Token(sym.TRUE, yyline+1, yycolumn+1, yytext(), yytext());}
	"false"	{return new Token(sym.FALSE, yyline+1, yycolumn+1, yytext(), yytext());}
	"null"	{return new Token(sym.NULL, yyline+1, yycolumn+1, yytext(), yytext());}

	{CLASS_ID}	{return new Token(sym.CLASS_ID, yyline+1, yycolumn+1, "CLASS_ID", yytext());}
	{ID}	{return new Token(sym.ID, yyline+1, yycolumn+1, "ID", yytext());}
	{INTEGER}	{return new Token(sym.INTEGER, yyline+1, yycolumn+1, "INTEGER", yytext());}

	{COMMENT_LINE} {}
	{COMMENT_MULTILINE} { yybegin(COMMENT); }

	{STRING} {return new Token(sym.STRING, yyline+1, yycolumn+1, "STRING", yytext());}
	
	"=="                           {return new Token(sym.EQUAL, yyline+1, yycolumn+1, yytext(), yytext());}
	"="                            {return new Token(sym.ASSIGNMENT, yyline+1, yycolumn+1, yytext(), yytext());}
	"<="                           {return new Token(sym.SMALLER_OR_EQUAL, yyline+1, yycolumn+1, yytext(), yytext());}
	">="                           {return new Token(sym.GREATER_OR_EQUAL, yyline+1, yycolumn+1, yytext(), yytext());}
	">"                            {return new Token(sym.GREATER, yyline+1, yycolumn+1, yytext(), yytext());}
	"<"                            {return new Token(sym.SMALLER, yyline+1, yycolumn+1, yytext(), yytext());}
	"!="                           {return new Token(sym.NOT_EQUAL, yyline+1, yycolumn+1, yytext(), yytext());}
	"!"                            {return new Token(sym.NOT, yyline+1, yycolumn+1, yytext(), yytext());}
	"&&"                           {return new Token(sym.AND, yyline+1, yycolumn+1, yytext(), yytext());}
	"||"                           {return new Token(sym.OR, yyline+1, yycolumn+1, yytext(), yytext());}
	"+"                            {return new Token(sym.PLUS, yyline+1, yycolumn+1, yytext(), yytext());}
	"-"                            {return new Token(sym.MINUS, yyline+1, yycolumn+1, yytext(), yytext());}
	"."								{return new Token(sym.DOT, yyline+1, yycolumn+1, yytext(), yytext());}
	","								{return new Token(sym.COMMA, yyline+1, yycolumn+1, yytext(), yytext());}
	";"								{return new Token(sym.DELIMITER, yyline+1, yycolumn+1, yytext(), yytext());}
	"["								{return new Token(sym.LEFT_SQUARE_BRACKETS, yyline+1, yycolumn+1, yytext(), yytext());}
	"]"								{return new Token(sym.RIGHT_SQUARE_BRACKETS, yyline+1, yycolumn+1, yytext(), yytext());}
	"("								{return new Token(sym.LEFT_PARENTHESES, yyline+1, yycolumn+1, yytext(), yytext());}
	")"								{return new Token(sym.RIGHT_PARENTHESES, yyline+1, yycolumn+1, yytext(), yytext());}
	"*"								{return new Token(sym.MULTIFICATION, yyline+1, yycolumn+1, yytext(), yytext());}
	"/"								{return new Token(sym.DIVISION, yyline+1, yycolumn+1, yytext(), yytext());}
	"%"								{return new Token(sym.MODULO, yyline+1, yycolumn+1, yytext(), yytext());}
	"{"								{return new Token(sym.LEFT_CUR_PARENTHESES, yyline+1, yycolumn+1, yytext(), yytext());}
	"}"								{return new Token(sym.RIGHT_CUR_PARENTHESES, yyline+1, yycolumn+1, yytext(), yytext());}
	. 								{ throw new LexicalError(yyline+1, yycolumn+1, "unsupported symbol found: \"" +yytext()+"\""); }
}
<COMMENT> {
      "*/" { yybegin(YYINITIAL); }
      . | {WHITESPACE} {}
      <<EOF>>	{ throw new LexicalError(yyline+1, yycolumn+1, "unterminated comment"); }
    }