package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Array length expression AST node.
 * 
 * @author Tovi Almozlino
 */
public class Length extends Expression {

	private Expression array;

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
	 * Constructs a new array length expression node.
	 * 
	 * @param array
	 *            Expression representing an array.
	 */
	public Length(Expression array) {
		super(array.getLine());
		this.array = array;
	}

	public Expression getArray() {
		return array;
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
	@Override
	public int setAndGetRegWeight() {
		if (this.regWeight != -2)
			return this.regWeight;
		
		int exprWeight = this.getArray().setAndGetRegWeight();
		this.regWeight = Expression.calcInternalRegWeight(exprWeight, 1);
		return this.regWeight;
	}
}
