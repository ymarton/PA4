package IC.AST;

import java.util.ArrayList;
import java.util.List;

import IC.Semantic.ThrowingVisitor;

/**
 * Library method declaration AST node.
 * 
 * @author Tovi Almozlino
 */
public class LibraryMethod extends Method {

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
	 * Constructs a new library method declaration node.
	 * 
	 * @param type
	 *            Data type returned by method.
	 * @param name
	 *            Name of method.
	 * @param formals
	 *            List of method parameters.
	 */
	public LibraryMethod(Type type, String name, List<Formal> formals) {
		super(type, name, formals, new ArrayList<Statement>());
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