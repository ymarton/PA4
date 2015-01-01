package IC.AST;

import IC.DataTypes;
import IC.Semantic.ThrowingVisitor;

/**
 * Primitive data type AST node.
 * 
 * @author Tovi Almozlino
 */
public class PrimitiveType extends Type {

	private DataTypes type;

	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}
	public Object accept(ThrowingVisitor visitor) {
		try {
			return visitor.visit(this);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(0);
			return null;
		}
		
	}
	/**
	 * Constructs a new primitive data type node.
	 * 
	 * @param line
	 *            Line number of type declaration.
	 * @param type
	 *            Specific primitive data type.
	 */
	public PrimitiveType(int line, DataTypes type) {
		super(line);
		this.type = type;
	}

	public String getName() {
		return type.getDescription();
	}
	public <D, U> U accept(PropagatingVisitor<D, U> visitor, D context) {
		try
		{
			return visitor.visit(this, context);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(0);
			return null;
		}
	}
	
	public DataTypes getInternalEnum()
	{
		return this.type;
	}
	
	public static PrimitiveType cloneTypeButResetDimTo1(PrimitiveType type)
	{
		PrimitiveType clonedType = new PrimitiveType(type.getLine(), type.getInternalEnum());
		clonedType.incrementDimension(); // now dim == 1
		return clonedType;
	}
	
	public static PrimitiveType cloneTypeDecreaseOneDim(PrimitiveType type)
	{
		PrimitiveType clonedType = new PrimitiveType(type.getLine(), type.getInternalEnum());
		int orgDim = type.getDimension();
		
		for (int i=0; i< orgDim-1; i++)
			clonedType.incrementDimension();
		return clonedType;
	}
}