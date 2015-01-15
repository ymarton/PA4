package IC.Symbols;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import IC.Semantic.ClassNode;
import IC.Semantic.ClassesGraph;

public class SymbolTable
{
	static private String programFilename = "";
	static private SymbolTable topTable = null;
	
	/** map from String to Symbol **/
	private Map<String,Symbol> entries;
	private String id;
	private SymbolTable parentSymbolTable;
	private int increasingGUID;
	private String scopeGUID;
	
	/**
	 * set global pointer to the global table
	 */
	
	public String getScopeGUID()
	{
		return this.scopeGUID;
	}
	public static void setTopTable(SymbolTable topTable)
	{
		SymbolTable.topTable = topTable;
	}
	
	public static SymbolTable getTopTable()
	{
		return SymbolTable.topTable;
	}
	
	public SymbolTable getParentTable()
	{
		return this.parentSymbolTable;
	}

	public Symbol getSymbolByID(String ID)
	{
		return this.entries.get(ID);
	}
	public Collection<Symbol> getSymbols()
	{
		return this.entries.values();
	}

	/**
	 * if a derived class is extending another super class, the parent of the derived class symbol table is the super symbol table
	 * we can set it only after building entire class symbol tables (for example current built class symbol table is extending the next ClassDecl in file)
	 */
	public static void setClassesScopesParentsToSupers(SymbolTable programTable)
	{
		if (!programTable.id.equals("program"))
			return;

		for (Symbol classSymbol : programTable.getSymbols())
		{
			ClassNode classNode = ClassesGraph.getClassNode(classSymbol.getId());
			String superName = classNode.getSuperName();
			if (superName != null)
			{
				Symbol superSymbol = programTable.getSymbolByID(superName);
				classSymbol.getSymTableRef().attachParentTable(superSymbol.getSymTableRef());
			}
		}
	}
	public static void setProgFilename(String filename)
	{
		SymbolTable.programFilename = filename;
	}

	public SymbolTable(String id) {
		this.id = id;
		this.entries = new LinkedHashMap<String,Symbol>();
		this.parentSymbolTable = null;
		this.increasingGUID = 0;
		this.scopeGUID = UUID.randomUUID().toString();
	}

	public void AddSymbol(Symbol symbol)
	{
		entries.put(symbol.getId(), symbol);
	}

	/* used for unique id for statement blocks! */
	public Integer getGUID()
	{
		this.increasingGUID++;
		return this.increasingGUID;
	}

	public boolean isFirstDecleration(String id)
	{
		return !this.entries.containsKey(id);
	}

	public void attachParentTable(SymbolTable parent)
	{
		this.parentSymbolTable = parent;
	}

	public String getNiceTableOut(Kind ownerKind)
	{
		String out = "";
		String rest = "";
		String children = "";
		switch (ownerKind) {
		case PROGRAM:
			out += "Global Symbol Table: " + SymbolTable.programFilename;
			for (Symbol entry : this.entries.values())
			{
				out += "\n    " + entry.getNiceOut();
				if (!rest.isEmpty())
					rest += "\n";
				rest += "\n" + entry.getSymTableRef().getNiceTableOut(entry.getKind());

				// 
				if (entry.getSymTableRef().getParentTable().getID().equals(this.getID()))
				{
					if (!children.isEmpty())
						children += ", ";
					children += entry.getId();
				}
			}
			if (!children.isEmpty())
				out += "\nChildren tables: " + children;
			if (!rest.isEmpty())
				out += "\n" + rest;
			return out;
		case CLASS:
			out += "Class Symbol Table: " + this.id;
			String fields = "";
			String methods ="";
			for (Symbol entry : this.entries.values())
			{
				if (entry.getKind() == Kind.FIELD_ATTRIBUTE)
					fields += "\n    " + entry.getNiceOut();
				else if (entry.getKind() == Kind.METHOD)
				{
					methods += "\n    " + entry.getNiceOut();
					if (!rest.isEmpty())
						rest += "\n";
					rest += "\n" + entry.getSymTableRef().getNiceTableOut(entry.getKind());
					if (!children.isEmpty())
						children += ", ";
					children += entry.getSymTableRef().getID();
				}
			}
			// add subclasses also as children tables
			ClassNode classNode = ClassesGraph.getClassNode(this.id);
			for (String derivedClassName : classNode.getNeighboursList().keySet()) {
				if (!children.isEmpty())
					children += ", ";
				children += derivedClassName;
			}
			if (!fields.isEmpty())
				out += fields;
			if (!methods.isEmpty())
				out += methods;
			if (!children.isEmpty())
				out += "\nChildren tables: " + children;
			if (!rest.isEmpty())
				out += "\n" + rest;

			return out;
		case METHOD:
			out += "Method Symbol Table: " + this.id;
			String params = "";
			String locals ="";
			for (Symbol entry : this.entries.values())
			{
				if (entry.getKind() == Kind.FORMAL_PARAM)
					params += "\n    " + entry.getNiceOut();
				else if (entry.getKind() == Kind.VAR_LOCAL)
					locals += "\n    " + entry.getNiceOut();
				else if (entry.getKind() == Kind.STMTS_BLOCK)
				{
					if (!rest.isEmpty())
						rest += "\n";
					rest += "\n" + entry.getSymTableRef().getNiceTableOut(entry.getKind());
					if (!children.isEmpty())
						children += ", ";
					children += entry.getSymTableRef().getID();
				}
			}
			if (!params.isEmpty())
				out += params;
			if (!locals.isEmpty())
				out += locals;
			if (!children.isEmpty())
				out += "\nChildren tables: " + children;
			if (!rest.isEmpty())
				out += "\n" + rest;

			return out;
		case STMTS_BLOCK:
			out += "Statement Block Symbol Table ( located in " + this.id.replaceFirst("statement block in ", "") +" )";
			for (Symbol entry : this.entries.values())
			{
				if (!rest.isEmpty())
					rest += "\n";
				rest += "\n    " + entry.getNiceOut();
				if (entry.getKind() == Kind.STMTS_BLOCK)
				{
					if (!children.isEmpty())
						children += ", ";
					children += entry.getSymTableRef().getID();
				}
			}
			if (!children.isEmpty())
				out += "\nChildren tables: " + children;
			if (!rest.isEmpty())
				//out += "\n" + rest;
				out += "\n" + rest;
			return out;
		default:
			return out;
		}
	}

	public String getID()
	{
		return this.id;
	}

	public boolean isIDDeclaredAndSeenVarFormalFieldType(String ID) {
		if (this.entries.containsKey(ID))
		{
			Symbol symbol = this.entries.get(ID);
			Kind kind = symbol.getKind();
			return ( symbol.isSeen() && ((kind == Kind.FIELD_ATTRIBUTE) || (kind == Kind.FORMAL_PARAM) || (kind == Kind.VAR_LOCAL)) );
		}
		return false;
	}

	public boolean isIDDeclaredAndSeenVarFormalType(String ID) {
		if (this.entries.containsKey(ID))
		{
			Symbol symbol = this.entries.get(ID);
			Kind kind = symbol.getKind();
			return ( symbol.isSeen() && ((kind == Kind.FORMAL_PARAM) || (kind == Kind.VAR_LOCAL)) );
		}
		return false;
	}

		
	public boolean isDeclaredMethod(String methodName, Property property) {
		if (this.entries.containsKey(methodName))
		{
			Symbol symbolFound = this.entries.get(methodName);
			return (symbolFound.getKind() == Kind.METHOD) && (symbolFound.getExtraProperty() == property);
		}
		return false;
	}
}
