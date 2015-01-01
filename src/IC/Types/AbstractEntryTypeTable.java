package IC.Types;

import IC.AST.Type;

public abstract class AbstractEntryTypeTable {
	
	/* with ABSTRACT - we can *force* the derived classes to override those Object's methods
	 * (can't do it with interfaces)
	 * by overriding the default we'll get CORRECT behavior,
	 * when we'll use derived classes instances in Hash based data structures
	 */
	
	public abstract boolean equals(Object obj);
	public abstract int hashCode();
	
	/* several print functions, differ in format */
	public abstract String getformattedEntry();
	
	public abstract String getTypeDescrClean();
	
	public static String niceOutForType(Type t)
	{
		if (t.getDimension() == 0)
			return t.getName();
		
		String brackets = new String(new char[t.getDimension()]).replace("\0", "[]");
		return t.getName() + brackets;
	}
}
