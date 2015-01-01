package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Method parameter AST node.
 * 
 * @author Tovi Almozlino
 */
public class Formal extends ASTNode {

	private Type type;

	private String name;

	public Object accept(ThrowingVisitor visitor) {
		try {
			return visitor.visit(this);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(0);
			return null;
		}
		
	}
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	/**
	 * Constructs a new parameter node.
	 * 
	 * @param type
	 *            Data type of parameter.
	 * @param name
	 *            Name of parameter.
	 */
	public Formal(Type type, String name) {
		super(type.getLine());
		this.type = type;
		this.name = name;
	}

	public Type getType() {
		return type;
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
}
