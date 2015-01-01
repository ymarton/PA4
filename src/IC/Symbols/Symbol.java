package IC.Symbols;

import IC.Types.AbstractEntryTypeTable;

public class Symbol
{
	private String id;
	private Kind kind;
	private AbstractEntryTypeTable type;
	private SymbolTable childSymbolTable;
	private Property extraProperty;
	private String cleanID; // used only for statement blocks (for uniqueness the id is numbered guid), will contain "statement block in..."
	private boolean alreadySeenAtScopeCheck; // used in scope checks - ensure that if the used id is declared in the same scope - the dec. statement is 8before* the checked statement
	
	/**
	 * @param id - symbol id
	 * @param kind - symbol kind (local_var, method etc)
	 * @param type - symbol type (the object type / method signature)
	 * @param isOpenScope - is symbol opens(owned) a symbol table (like class, method etc...)
	 * @param extraProperty - main usage for methods (static, virtual etc)
	 * @param cleanID - symbol ids have to be unique, so two following statement blocks will have numbers to differ,
	 * 					but both open a symbol table that its id should be the same "statement block in"
	 */
	public Symbol(String id, Kind kind, AbstractEntryTypeTable type, boolean isOpenScope)
	{
		this(id, kind, type, isOpenScope, null, null);
	}

	public Symbol(String id, Kind kind, AbstractEntryTypeTable type, boolean isOpenScope, Property extraProperty)
	{
		this(id, kind, type, isOpenScope, extraProperty, null);
	}

	public Symbol(String id, Kind kind, AbstractEntryTypeTable type, boolean isOpenScope, Property extraProperty, String cleanID)
	{
		this.id = id;
		this.kind = kind;
		this.type = type;
		this.extraProperty = (extraProperty != null) ? extraProperty : Property.NULL;
		this.cleanID = (cleanID != null) ? cleanID : id;
		this.alreadySeenAtScopeCheck = false;
		
		if (isOpenScope)
			this.childSymbolTable = new SymbolTable(this.cleanID);
		
		if (this.kind == Kind.FIELD_ATTRIBUTE)
			this.alreadySeenAtScopeCheck = true;
	}

	public Property getExtraProperty()
	{
		return this.extraProperty;
	}
	
	protected void updateType(AbstractEntryTypeTable type)
	{
		this.type = type;
	}

	public void markAsSeen()
	{
		this.alreadySeenAtScopeCheck = true;
	}
	
	public String getId()
	{
		return this.id;
	}

	public AbstractEntryTypeTable getTypeEntry()
	{
		return this.type;
	}

	public Kind getKind()
	{
		return this.kind;
	}

	public SymbolTable getSymTableRef()
	{
		return this.childSymbolTable;
	}

	/**
	 * @return string containing the out for the symbol table as expected in the PA examples
	 */
	public String getNiceOut()
	{
		switch (this.kind) {
		case CLASS:
			return "Class: " + this.id;
		case METHOD:
			String mType = (this.extraProperty == Property.VIRTUAL) ? "Virtual" : "Static";
			return mType + " method: " + this.getId() + " " + this.type.getTypeDescrClean();
		case FIELD_ATTRIBUTE:
			return "Field: " + this.type.getTypeDescrClean() + " " + this.getId();
		case FORMAL_PARAM:
			return "Parameter: " + this.type.getTypeDescrClean() + " " + this.getId();
		case VAR_LOCAL:
			return "Local variable: " + this.type.getTypeDescrClean() + " " + this.getId();
		case STMTS_BLOCK:
			return "\n" + this.getSymTableRef().getNiceTableOut(this.getKind());
		default:
			return "";
		}
	}
	
	public boolean isSeen()
	{
		return this.alreadySeenAtScopeCheck;
	}
}

