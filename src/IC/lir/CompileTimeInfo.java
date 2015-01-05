package IC.lir;

import java.util.HashMap;
import java.util.Map;

public class CompileTimeInfo {
	
	private static Map<String,ClassLayout> classLayouts = new HashMap<String, ClassLayout>();
	
	private CompileTimeInfo() {}
	
	public static ClassLayout getClassLayout(String className)
	{
		return classLayouts.get(className);
	}
	
	public static void addClassLayout(String className, ClassLayout layout)
	{
		classLayouts.put(className, layout);
	}
	
	public static boolean isAlreadyBuilt(String className)
	{
		return classLayouts.containsKey(classLayouts);
	}
}
