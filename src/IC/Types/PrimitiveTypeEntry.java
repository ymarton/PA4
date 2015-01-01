package IC.Types;

import IC.DataTypes;
import IC.LiteralTypes;

/**
 * represent the base primitive types, without dimension
 *
 */
public class PrimitiveTypeEntry extends AbstractEntryTypeTable {

	private int entryID;
	private PrimitiveTypeEnum type;
	private String desc;
	
	PrimitiveTypeEntry(PrimitiveTypeEnum primitive)
	{
		this(primitive, 0);
	}
	
	public PrimitiveTypeEntry(PrimitiveTypeEnum primitive, int entryID)
	{
		this.type = primitive;
		this.entryID = entryID;
		
		switch (this.type) {
		case INT:
			this.desc = "int";
			break;
		case STRING:
			this.desc = "string";
			break;
		case BOOLEAN:
			this.desc = "boolean";
			break;
		case VOID:
			this.desc = "void";
			break;
		case NULL:
			this.desc = "null";
			break;
		}
	}
		
	
	public static PrimitiveTypeEnum dataTypeToEnum(DataTypes dataType)
	{
		switch (dataType) {
		case INT:
			return PrimitiveTypeEnum.INT;
		case STRING:
			return PrimitiveTypeEnum.STRING;
			
		case BOOLEAN:
			return PrimitiveTypeEnum.BOOLEAN;
			
		case VOID:
			return PrimitiveTypeEnum.VOID;
			
		}
		return null;
	}
	
	public static PrimitiveTypeEnum literalTypeToEnum(LiteralTypes literalType)
	{
		switch (literalType) {
		case INTEGER:
			return PrimitiveTypeEnum.INT;
			
		case STRING:
			return PrimitiveTypeEnum.STRING;
			
		case TRUE:
			return PrimitiveTypeEnum.BOOLEAN;
			
		case FALSE:
			return PrimitiveTypeEnum.BOOLEAN;
			
		case NULL:
			return PrimitiveTypeEnum.NULL;
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PrimitiveTypeEntry))
			return false;
		if (obj == this)
			return true;
		
		PrimitiveTypeEntry p = (PrimitiveTypeEntry)obj;
		return (this.type == p.type);
	}

	@Override
	public int hashCode() {
		return this.desc.hashCode();
	}

	@Override
	public String getformattedEntry() {
		return String.valueOf(this.entryID) + ": Primitive type: " + this.desc;
	}
	@Override
	public String getTypeDescrClean() {
		return this.desc;
	}
}
