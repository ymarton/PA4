package IC.AST;

public interface PropagatingVisitor<D,U>
{
	/** Visits a statement node with a given context object (book-keeping)
	 * and returns the result of the computation on this node */ 

	public U visit(Program program, D context) throws Exception;

	public U visit(ICClass icClass, D context) throws Exception;

	public U visit(Field field, D context) throws Exception;

	public U visit(VirtualMethod method, D context) throws Exception;

	public U visit(StaticMethod method, D context) throws Exception;

	public U visit(LibraryMethod method, D context) throws Exception;

	public U visit(Formal formal, D context) throws Exception;

	public U visit(PrimitiveType type, D context) throws Exception;

	public U visit(UserType type, D context) throws Exception;

	public U visit(Assignment assignment, D context) throws Exception;

	public U visit(CallStatement callStatement, D context) throws Exception;

	public U visit(Return returnStatement, D context) throws Exception;

	public U visit(If ifStatement, D context) throws Exception;

	public U visit(While whileStatement, D context) throws Exception;

	public U visit(Break breakStatement, D context) throws Exception;

	public U visit(Continue continueStatement, D context) throws Exception;

	public U visit(StatementsBlock statementsBlock, D context) throws Exception;

	public U visit(LocalVariable localVariable, D context) throws Exception;

	public U visit(VariableLocation location, D context) throws Exception;

	public U visit(ArrayLocation location, D context) throws Exception;

	public U visit(StaticCall call, D context) throws Exception;

	public U visit(VirtualCall call, D context) throws Exception;

	public U visit(This thisExpression, D context) throws Exception;

	public U visit(NewClass newClass, D context) throws Exception;

	public U visit(NewArray newArray, D context) throws Exception;

	public U visit(Length length, D context) throws Exception;

	public U visit(MathBinaryOp binaryOp, D context) throws Exception;

	public U visit(LogicalBinaryOp binaryOp, D context) throws Exception;

	public U visit(MathUnaryOp unaryOp, D context) throws Exception;

	public U visit(LogicalUnaryOp unaryOp, D context) throws Exception;

	public U visit(Literal literal, D context) throws Exception;

	public U visit(ExpressionBlock expressionBlock, D context) throws Exception;
}
