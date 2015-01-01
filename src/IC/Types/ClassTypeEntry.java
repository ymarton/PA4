package IC.Types;

/**
 * class type (dim == 0!)
 *
 */
public class ClassTypeEntry extends AbstractEntryTypeTable{

	private String name;
	private String superName;
	private int entryID;
	private int superID;
	
	public int getEntryID()
	{
		return this.entryID;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getSuperName()
	{
		return this.superName;
	}
	
	public void setSuperID(int superID)
	{
		this.superID = superID;
	}
	public ClassTypeEntry(String name, int entryID, String superName)
	{
		this(name, entryID);
		this.superName = superName;
	}
	
	public ClassTypeEntry(String name, int entryID)
	{
		this.name = name;
		this.entryID = entryID;
		this.superName = null;
	}
	
	public ClassTypeEntry(String name)
	{
		this(name, 0);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClassTypeEntry))
			return false;
		if (obj == this)
			return true;
		
		ClassTypeEntry c = (ClassTypeEntry)obj;
		return this.name.equals(c.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String getformattedEntry() {
		if (this.superName == null)
			return String.valueOf(this.entryID) + ": Class: " + this.name;
		return String.valueOf(this.entryID) + ": Class: " + this.name + ", Superclass ID: " + String.valueOf(this.superID);
	}

	@Override
	public String getTypeDescrClean() {
		return this.getName();
	}

	
}
