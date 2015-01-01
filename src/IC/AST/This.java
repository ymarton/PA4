package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * 'This' expression AST node.
 * 
 * @author Tovi Almozlino
 */
public class This extends Expression {

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
	 * Constructs a 'this' expression node.
	 * 
	 * @param line
	 *            Line number of 'this' expression.
	 */
	public This(int line) {
		super(line);
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
