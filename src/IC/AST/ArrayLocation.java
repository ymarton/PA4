package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Array reference AST node.
 * 
 * @author Tovi Almozlino
 */
public class ArrayLocation extends Location {

	private Expression array;

	private Expression index;

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
	 * Constructs a new array reference node.
	 * 
	 * @param array
	 *            Expression representing an array.
	 * @param index
	 *            Expression representing a numeric index.
	 */
	public ArrayLocation(Expression array, Expression index) {
		super(array.getLine());
		this.array = array;
		this.index = index;
	}

	public Expression getArray() {
		return array;
	}

	public Expression getIndex() {
		return index;
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