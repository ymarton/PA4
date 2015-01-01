package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Assignment statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class Assignment extends Statement {

	private Location variable;

	private Expression assignment;

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
	 * Constructs a new assignment statement node.
	 * 
	 * @param variable
	 *            Variable to assign a value to.
	 * @param assignment
	 *            Value to assign.
	 */
	public Assignment(Location variable, Expression assignment) {
		super(variable.getLine());
		this.variable = variable;
		this.assignment = assignment;
	}

	public Location getVariable() {
		return variable;
	}

	public Expression getAssignment() {
		return assignment;
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
