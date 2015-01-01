package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Return statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class Return extends Statement {

	private Expression value = null;

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
	 * Constructs a new return statement node, with no return value.
	 * 
	 * @param line
	 *            Line number of return statement.
	 */
	public Return(int line) {
		super(line);
	}

	/**
	 * Constructs a new return statement node.
	 * 
	 * @param line
	 *            Line number of return statement.
	 * @param value
	 *            Return value.
	 */
	public Return(int line, Expression value) {
		this(line);
		this.value = value;
	}

	public boolean hasValue() {
		return (value != null);
	}

	public Expression getValue() {
		return value;
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
