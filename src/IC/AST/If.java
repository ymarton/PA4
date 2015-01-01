package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * If statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class If extends Statement {

	private Expression condition;

	private Statement operation;

	private Statement elseOperation = null;

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
	 * Constructs an If statement node.
	 * 
	 * @param condition
	 *            Condition of the If statement.
	 * @param operation
	 *            Operation to perform if condition is true.
	 * @param elseOperation
	 *            Operation to perform if condition is false.
	 */
	public If(Expression condition, Statement operation, Statement elseOperation) {
		this(condition, operation);
		this.elseOperation = elseOperation;
	}

	/**
	 * Constructs an If statement node, without an Else operation.
	 * 
	 * @param condition
	 *            Condition of the If statement.
	 * @param operation
	 *            Operation to perform if condition is true.
	 */
	public If(Expression condition, Statement operation) {
		super(condition.getLine());
		this.condition = condition;
		this.operation = operation;
	}

	public Expression getCondition() {
		return condition;
	}

	public Statement getOperation() {
		return operation;
	}

	public boolean hasElse() {
		return (elseOperation != null);
	}

	public Statement getElseOperation() {
		return elseOperation;
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
