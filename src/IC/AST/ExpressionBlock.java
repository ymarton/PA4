package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * AST node for expression in parentheses.
 * 
 * @author Tovi Almozlino
 */
public class ExpressionBlock extends Expression {

	private Expression expression;

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
	 * Constructs a new expression in parentheses node.
	 * 
	 * @param expression
	 *            The expression.
	 */
	public ExpressionBlock(Expression expression) {
		super(expression.getLine());
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
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
		
		this.regWeight = this.expression.setAndGetRegWeight();
		return this.regWeight;
	}
}
