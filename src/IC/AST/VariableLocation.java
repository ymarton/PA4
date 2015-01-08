package IC.AST;

import IC.Semantic.ThrowingVisitor;

/**
 * Variable reference AST node.
 * 
 * @author Tovi Almozlino
 */
public class VariableLocation extends Location {

	private Expression location = null;

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
	 * Constructs a new variable reference node.
	 * 
	 * @param line
	 *            Line number of reference.
	 * @param name
	 *            Name of variable.
	 */
	public VariableLocation(int line, String name) {
		super(line);
		this.name = name;
	}

	/**
	 * Constructs a new variable reference node, for an external location.
	 * 
	 * @param line
	 *            Line number of reference.
	 * @param location
	 *            Location of variable.
	 * @param name
	 *            Name of variable.
	 */
	public VariableLocation(int line, Expression location, String name) {
		this(line, name);
		this.location = location;
	}

	public boolean isExternal() {
		return (location != null);
	}

	public Expression getLocation() {
		return location;
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
	@Override
	public int setAndGetRegWeight() {
		if (this.regWeight != -2)
			return this.regWeight;
		
		if (!this.isExternal())
			this.regWeight = 1;
		else
		{
			int locationWeight = this.location.setAndGetRegWeight();
			this.regWeight = Expression.calcInternalRegWeight(locationWeight, 1);
		}
		return this.regWeight;
	}
}
