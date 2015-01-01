package IC.AST;
import IC.Semantic.ThrowingVisitor;
import IC.Symbols.SymbolTable;
import IC.Types.AbstractEntryTypeTable;

/**
 * Abstract AST node base class.
 * 
 * @author Tovi Almozlino
 */
public abstract class ASTNode {

	/** reference to symbol table of enclosing scope **/
	private SymbolTable enclosingScope;
	private AbstractEntryTypeTable assignedType;
	
	private int line;

	/**
	 * Double dispatch method, to allow a visitor to visit a specific subclass.
	 * 
	 * @param visitor
	 *            The visitor.
	 * @return A value propagated by the visitor.
	 */

	public abstract Object accept(ThrowingVisitor visitor);
	
	public abstract Object accept(Visitor visitor);

	public abstract <D,U> U accept(PropagatingVisitor<D,U> visitor,D context);

	/**
	 * Constructs an AST node corresponding to a line number in the original
	 * code. Used by subclasses.
	 * 
	 * @param line
	 *            The line number.
	 */
	protected ASTNode(int line) {
		this.line = line;
		this.enclosingScope = null;
		this.assignedType = null;
	}

	public AbstractEntryTypeTable getAssignedType()
	{
		return this.assignedType;
	}
	
	public void setAssignedType(AbstractEntryTypeTable evaluatedType)
	{
		this.assignedType = evaluatedType;
	}
	
	public int getLine() {
		return line;
	}

	/** returns symbol table of enclosing scope **/
	public SymbolTable getEnclosingScope() {
		return enclosingScope;
	}

	/** sets symbol table of enclosing scope **/
	public void setEnclosingScope(SymbolTable parent) {
		this.enclosingScope = parent;
	}
}
