package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * User-defined data type AST node.
 * 
 * @author Tovi Almozlino
 */
public class UserType extends Type {

	private String name;

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
	 * Constructs a new user-defined data type node.
	 * 
	 * @param line
	 *            Line number of type declaration.
	 * @param name
	 *            Name of data type.
	 */
	public UserType(int line, String name) {
		super(line);
		this.name = name;
	}

	public String getName() {
		return name;
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
	
	public static UserType cloneTypeButResetDimTo1(UserType type)
	{
		UserType clonedType = new UserType(type.getLine(), type.getName());
		clonedType.incrementDimension();
		return clonedType;
	}
	
	public static UserType cloneTypeDecreaseOneDim(UserType type)
	{
		UserType clonedType = new UserType(type.getLine(), type.getName());
		int orgDim = type.getDimension();
		
		for (int i=0; i< orgDim-1; i++)
			clonedType.incrementDimension();
		return clonedType;
	}
}
