package IC.Semantic;

import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.CallStatement;
import IC.AST.Continue;
import IC.AST.ExpressionBlock;
import IC.AST.Field;
import IC.AST.Formal;
import IC.AST.ICClass;
import IC.AST.If;
import IC.AST.Length;
import IC.AST.LibraryMethod;
import IC.AST.Literal;
import IC.AST.LocalVariable;
import IC.AST.LogicalBinaryOp;
import IC.AST.LogicalUnaryOp;
import IC.AST.MathBinaryOp;
import IC.AST.MathUnaryOp;
import IC.AST.NewArray;
import IC.AST.NewClass;
import IC.AST.PrimitiveType;
import IC.AST.Program;
import IC.AST.Return;
import IC.AST.StatementsBlock;
import IC.AST.StaticCall;
import IC.AST.StaticMethod;
import IC.AST.This;
import IC.AST.UserType;
import IC.AST.VariableLocation;
import IC.AST.VirtualCall;
import IC.AST.VirtualMethod;
import IC.AST.While;


/**
 * AST visitor interface. Declares methods for visiting each type of AST node.
 * Difference from visitor interface - this one can throw exceptions
 * 
 * @author Tovi Almozlino
 */
public interface ThrowingVisitor {

	public Object visit(Program program) throws Exception;

	public Object visit(ICClass icClass) throws Exception;

	public Object visit(Field field) throws Exception;

	public Object visit(VirtualMethod method) throws Exception;

	public Object visit(StaticMethod method) throws Exception;

	public Object visit(LibraryMethod method) throws Exception;

	public Object visit(Formal formal) throws Exception;

	public Object visit(PrimitiveType type) throws Exception;

	public Object visit(UserType type) throws Exception;

	public Object visit(Assignment assignment) throws Exception;

	public Object visit(CallStatement callStatement) throws Exception;

	public Object visit(Return returnStatement) throws Exception;

	public Object visit(If ifStatement) throws Exception;

	public Object visit(While whileStatement) throws Exception;

	public Object visit(Break breakStatement) throws Exception;

	public Object visit(Continue continueStatement) throws Exception;

	public Object visit(StatementsBlock statementsBlock)throws Exception;

	public Object visit(LocalVariable localVariable)throws Exception;

	public Object visit(VariableLocation location)throws Exception;

	public Object visit(ArrayLocation location) throws Exception;

	public Object visit(StaticCall call) throws Exception;

	public Object visit(VirtualCall call) throws Exception;

	public Object visit(This thisExpression) throws Exception;

	public Object visit(NewClass newClass) throws Exception;

	public Object visit(NewArray newArray) throws Exception;

	public Object visit(Length length) throws Exception;

	public Object visit(MathBinaryOp binaryOp) throws Exception;

	public Object visit(LogicalBinaryOp binaryOp) throws Exception;

	public Object visit(MathUnaryOp unaryOp) throws Exception;

	public Object visit(LogicalUnaryOp unaryOp) throws Exception;

	public Object visit(Literal literal) throws Exception;

	public Object visit(ExpressionBlock expressionBlock) throws Exception;
}
