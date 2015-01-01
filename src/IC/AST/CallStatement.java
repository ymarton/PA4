package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Method call statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class CallStatement extends Statement {

	private Call call;

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
	 * Constructs a new method call statement node.
	 * 
	 * @param call
	 *            Method call expression.
	 */
	public CallStatement(Call call) {
		super(call.getLine());
		this.call = call;
	}

	public Call getCall() {
		return call;
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
