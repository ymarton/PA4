package IC.lir;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import IC.lir.Instructions.DispatchTable;

public class CompileTimeData {
	
	private static Map<String,ClassLayout> classLayouts = new HashMap<String, ClassLayout>();
    private static HashMap<String, Integer> stringLiterals = new LinkedHashMap<String, Integer>();
    private static List<DispatchTable> dispatchTables = new LinkedList<DispatchTable>();
    
	private CompileTimeData() {}
	
	public static ClassLayout getClassLayout(String className)
	{
		return classLayouts.get(className);
	}
	
	public static boolean isRegName(String candidate)
	{
		if (!candidate.startsWith("R"))
			return false;
		candidate = candidate.replaceFirst("R", "");
		try {
			int regnum = Integer.parseInt(candidate);
			if (regnum < 0)
				return false;
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static boolean isImmediate(String candidate)
	{
		try {
			Integer.parseInt(candidate);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isMemory(String candidate)
	{
		return !(isRegName(candidate) || isImmediate(candidate));
	}
	public static List<String> getStringLiterals()
	{
		List <String> literalsList = new LinkedList<String>();
		for (Entry<String, Integer> stringLiteral : stringLiterals.entrySet()) {
			literalsList.add("str" + stringLiteral.getValue() + ": " + stringLiteral.getKey() + "\n");
		}
		return literalsList;
	}
	
	
	public static List<String> getDispatchTables()
	{
		List <String> dtList = new LinkedList<String>();
		for (DispatchTable dt : dispatchTables) {
			dtList.add(dt.toString());
		}
		return dtList;
	}
	
	public static void addDispatchTable(DispatchTable dispatchTable)
	{
		dispatchTables.add(dispatchTable);
	}
	
	public static void addClassLayout(String className, ClassLayout layout)
	{
		classLayouts.put(className, layout);
	}
	
	public static String addStringLiteralGetSymbol(String str)
	{
		int index;
		if (!stringLiterals.containsKey(str))
		{
			index  = stringLiterals.size()+1;
			stringLiterals.put(str, index);
		}
		else
			index = stringLiterals.get(str);
		
		return "str" + index;
	}
	
	public static boolean isAlreadyBuilt(String className)
	{
		return classLayouts.containsKey(className);
	}
}
