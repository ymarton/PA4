package IC.Types;
import IC.AST.Type;

/**
 * array type (base can be primitive or user)
 *
 */
public class ArrayTypeEntry extends AbstractEntryTypeTable {

	private Type entry;
	private int entryID;
	
	public ArrayTypeEntry(Type type)
	{
		this(type, 0);
	}
	
	public ArrayTypeEntry(Type type, int entryID)
	{
		this.entry = type;
		this.entryID = entryID;
	}

	public Type getType()
	{
		return this.entry;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ArrayTypeEntry))
			return false;
		if (obj == this)
			return true;
		
		ArrayTypeEntry a = (ArrayTypeEntry)obj;
		return ( (this.entry.getName().equals(a.entry.getName())) && (this.entry.getDimension() == a.entry.getDimension()) );
	}

	@Override
	public int hashCode() {
		 return (this.entry.getName() + "$" + String.valueOf(this.entry.getDimension())).hashCode();
	}

	@Override
	public String getformattedEntry() {
		return String.valueOf(this.entryID) + ": Array type: " + ArrayTypeEntry.niceOutForType(this.entry);
	}

	@Override
	public String getTypeDescrClean() {
		return ArrayTypeEntry.niceOutForType(this.entry);
	}
	
}
