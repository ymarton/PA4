package IC.Types;

import java.util.LinkedList;
import java.util.List;

import IC.AST.Formal;
import IC.AST.Type;

/**
 * represent a method signature (return type + formals types)
 *
 */
public class MethodTypeEntry extends AbstractEntryTypeTable {

	private Type retType;
	private List<Type> params;
	private int entryID;
	
	public MethodTypeEntry(Type retType, List<Formal> formalsList, int entryID)
	{
		this.retType = retType;
		this.params = new LinkedList<Type>();
		this.entryID = entryID;
		
		for (Formal formal : formalsList)
		{
			this.params.add(formal.getType());
		}
	}
	
	private String getParamsRepr()
	{
		String repr = "";
		for (Type param : this.params)
		{
			if (!repr.isEmpty())
				repr += ",";
			repr += param.getName() + "$" + String.valueOf(param.getDimension());
		}
		
		return repr;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodTypeEntry))
			return false;
		if (obj == this)
			return true;
		
		MethodTypeEntry m = (MethodTypeEntry)obj;
		
		return ( (this.retType.getName().equals(m.retType.getName()))
				&& (this.retType.getDimension() == m.retType.getDimension())
				&& (this.getParamsRepr().equals(m.getParamsRepr()))
			);
	}

	@Override
	public int hashCode() {
		return (this.retType.getName() + "$" + this.retType.getDimension()+ "$" + this.getParamsRepr()).hashCode();
	}

	@Override
	public String getformattedEntry() {
		String paramsFormatted = "";
		for (Type param : this.params)
		{
			if (!paramsFormatted.isEmpty())
				paramsFormatted += ", ";
			paramsFormatted += MethodTypeEntry.niceOutForType(param);
		}
		
		//return String.valueOf(this.entryID) +  ": Method type: {" + paramsFormatted + " -> " + MethodTypeEntry.niceOutForType(this.retType) + "}";
		return String.valueOf(this.entryID) +  ": Method type: " + this.getSignature();
	}

	public String getSignature()
	{
		String paramsFormatted = "";
		for (Type param : this.params)
		{
			if (!paramsFormatted.isEmpty())
				paramsFormatted += ", ";
			paramsFormatted += MethodTypeEntry.niceOutForType(param);
		}
		
		return "{" + paramsFormatted + " -> " + MethodTypeEntry.niceOutForType(this.retType) + "}";
		
	}

	@Override
	public String getTypeDescrClean() {
		return this.getSignature();
	}
	
	public List<Type> getParams()
	{
		return this.params;
	}

	public Type getRetType() {
		return this.retType;
	}
}
