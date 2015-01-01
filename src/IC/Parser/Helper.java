package IC.Parser;

import java.util.HashMap;
import java.util.Map;

public class Helper {

    public static Map<Integer,String> map = new HashMap<Integer, String>();;

    public static void checkIfIntIsLegal(String numAsString, boolean positive) throws NumberFormatException{
        if (!positive)
            numAsString = "-" + numAsString;
        new Integer(numAsString);
    }

    public static void fillMap() {
        map.put(new Integer(4), ";");
        map.put(new Integer(40), "<=");
        map.put(new Integer(42), ">=");
        map.put(new Integer(48), ":=");
        map.put(new Integer(11), "{");
        map.put(new Integer(49), "-");
        map.put(new Integer(10), "string");
        map.put(new Integer(45), "!=");
        map.put(new Integer(41), "<");
        map.put(new Integer(18), "integer literal");
        map.put(new Integer(43), ">");
        map.put(new Integer(16), "]");
        map.put(new Integer(26), "continue");
        map.put(new Integer(8), "int");
        map.put(new Integer(34), "-");
        map.put(new Integer(6), "static");
        map.put(new Integer(13), "(");
        map.put(new Integer(46), "!");
        map.put(new Integer(38), "and");
        map.put(new Integer(39), "or");
        map.put(new Integer(35), "*");
        map.put(new Integer(17), ",");
        map.put(new Integer(5), "class");
        map.put(new Integer(33), "+");
        map.put(new Integer(22), "if");
        map.put(new Integer(27), "this");
        map.put(new Integer(47), ".");
        map.put(new Integer(3), "identifier");
        map.put(new Integer(0), "end of file");
        map.put(new Integer(9), "boolean");
        map.put(new Integer(21), "return");
        map.put(new Integer(44), "==");
        map.put(new Integer(30), "true");
        map.put(new Integer(28), "new");
        map.put(new Integer(1), "error");
        map.put(new Integer(37), "%");
        map.put(new Integer(32), "null");
        map.put(new Integer(25), "break");
        map.put(new Integer(12), "}");
        map.put(new Integer(7), "void");
        map.put(new Integer(15), "[");
        map.put(new Integer(14), ")");
        map.put(new Integer(23), "else");
        map.put(new Integer(24), "while");
        map.put(new Integer(2), "class identifier");
        map.put(new Integer(20), "extends");
        map.put(new Integer(19), "string literal");
        map.put(new Integer(31), "false");
        map.put(new Integer(36), "/");
        map.put(new Integer(29), "length");
    }
}
