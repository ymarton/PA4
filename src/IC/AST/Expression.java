package IC.AST;

/**
 * Abstract base class for expression AST nodes.
 * 
 * @author Tovi Almozlino
 */
public abstract class Expression extends ASTNode {

	/**
	 * Constructs a new expression node. Used by subclasses.
	 * 
	 * @param line
	 *            Line number of expression.
	 */
	protected Expression(int line) {
		super(line);
	}
	
	// NO EVALUATION YET = -2
	// SELF OR SUBTREE CONTAIN SIDE EFFECTS = -1
	protected int regWeight = -2;
	
	public abstract int setAndGetRegWeight();
	
	public static int calcInternalRegWeight(int leftWeight, int rightWeight)
	{
		if ((leftWeight == -1) || (rightWeight == -1))
			return -1;
		
		if (leftWeight > rightWeight)
			return leftWeight;
		else if (rightWeight > leftWeight)
			return rightWeight;
		else
			return (leftWeight + 1);
	}
}