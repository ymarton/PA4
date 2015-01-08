package IC.lir;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import IC.lir.Instructions.DispatchTable;
import IC.lir.Instructions.LirLine;

public class CompileTimeData {
	
	private static Map<String,ClassLayout> classLayouts = new HashMap<String, ClassLayout>();
    private static List<LirLine> stringLiterals = new LinkedList<LirLine>();
    private static List<DispatchTable> dispatchTables = new LinkedList<DispatchTable>();
    
	private CompileTimeData() {}
	
	public static ClassLayout getClassLayout(String className)
	{
		return classLayouts.get(className);
	}
	
	public static List<LirLine> getStringLiterals()
	{
		return stringLiterals;
	}
	
	public static List<DispatchTable> getDispatchTables()
	{
		return dispatchTables;
	}
	
	public static void addDispatchTable(DispatchTable dispatchTable)
	{
		dispatchTables.add(dispatchTable);
	}
	
	public static void addClassLayout(String className, ClassLayout layout)
	{
		classLayouts.put(className, layout);
	}
	
	public static void addStringLiteral(LirLine stringLiteral)
	{
		stringLiterals.add(stringLiteral);
	}
	public static boolean isAlreadyBuilt(String className)
	{
		return classLayouts.containsKey(classLayouts);
	}
}
