package IC.AST;

import IC.Symbols.SymbolTable;

/**
 * Pretty printing visitor - travels along the AST and prints info about each
 * node, in an easy-to-comprehend format.
 */
public class PrettyPrinter implements Visitor {
	private int depth = 0; // depth of indentation

	private String ICFilePath;

	/**
	 * Constructs a new pretty printer visitor.
	 * 
	 * @param ICFilePath
	 *            The path + name of the IC file being compiled.
	 */
	public PrettyPrinter(String ICFilePath) {
		this.ICFilePath = ICFilePath;
	}

	private void indent(StringBuffer output, ASTNode node) {
		output.append("\n");
		for (int i = 0; i < depth; ++i)
			output.append("  ");
		if (node != null)
			output.append(node.getLine() + ": ");
	}

	private void indent(StringBuffer output) {
		indent(output, null);
	}

	private void appendTypeInfo(ASTNode node, boolean printType, StringBuffer output) {
		if (printType)
			output.append(String.format(", Type: %s", node.getAssignedType().getTypeDescrClean()));
		SymbolTable scope = node.getEnclosingScope();
		String table;
		if (scope.getParentTable() == null)
			table = "Global";
		else
			table = scope.getID();
		if (node instanceof ICClass && ((ICClass)node).hasSuperClass()) {
			table = ((ICClass)node).getSuperClassName();
		}
		output.append(", Symbol table: " + table);
	}

	public Object visit(Program program) {
		StringBuffer output = new StringBuffer();
		indent(output);
		output.append("Abstract Syntax Tree: " + ICFilePath + "\n");
		for (ICClass icClass : program.getClasses()) {
			if (!icClass.getName().equals("Library"))
				output.append(icClass.accept(this));
		}
		return output.toString();
	}

	public Object visit(ICClass icClass) {
		StringBuffer output = new StringBuffer();

		indent(output, icClass);
		output.append("Declaration of class: " + icClass.getName());
		if (icClass.hasSuperClass())
			output.append(", subclass of " + icClass.getSuperClassName());

		appendTypeInfo(icClass, true, output);

		depth += 2;
		for (Field field : icClass.getFields())
			output.append(field.accept(this));
		for (Method method : icClass.getMethods())
			output.append(method.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(PrimitiveType type) {
		StringBuffer output = new StringBuffer();

//		indent(output, type);
//		output.append("Primitive data type: ");
//		if (type.getDimension() > 0)
//			output.append(type.getDimension() + "-dimensional array of ");
//		output.append(type.getName());
		return output.toString();
	}

	public Object visit(UserType type) {
		StringBuffer output = new StringBuffer();

//		indent(output, type);
//		output.append("User-defined data type: ");
//		if (type.getDimension() > 0)
//			output.append(type.getDimension() + "-dimensional array of ");
//		output.append(type.getName());
		return output.toString();
	}

	public Object visit(Field field) {
		StringBuffer output = new StringBuffer();

		indent(output, field);
		output.append("Declaration of field: " + field.getName());
		appendTypeInfo(field, true, output);

		depth+=2;
		output.append(field.getType().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(LibraryMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of library method: " + method.getName());
		appendTypeInfo(method, true, output);

		depth += 2;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(Formal formal) {
		StringBuffer output = new StringBuffer();

		indent(output, formal);
		output.append("Parameter: " + formal.getName());
		appendTypeInfo(formal, true, output);

		depth+=2;
		output.append(formal.getType().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(VirtualMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of virtual method: " + method.getName());
		appendTypeInfo(method, true, output);

		depth += 2;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		for (Statement statement : method.getStatements())
			output.append(statement.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(StaticMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of static method: " + method.getName());
		appendTypeInfo(method, true, output);

		depth += 2;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		for (Statement statement : method.getStatements())
			output.append(statement.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(Assignment assignment) {
		StringBuffer output = new StringBuffer();

		indent(output, assignment);
		output.append("Assignment statement");
		appendTypeInfo(assignment, false, output);

		depth += 2;
		output.append(assignment.getVariable().accept(this));
		output.append(assignment.getAssignment().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(CallStatement callStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, callStatement);
		output.append("Method call statement");
		appendTypeInfo(callStatement, false, output);

		depth+=2;
		output.append(callStatement.getCall().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(Return returnStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, returnStatement);
		output.append("Return statement");
		if (returnStatement.hasValue())
			output.append(", with return value");
		appendTypeInfo(returnStatement, false, output);

		if (returnStatement.hasValue()) {
			depth+=2;
			output.append(returnStatement.getValue().accept(this));
			depth-=2;
		}
		return output.toString();
	}

	public Object visit(If ifStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, ifStatement);
		output.append("If statement");
		if (ifStatement.hasElse())
			output.append(", with Else operation");
		appendTypeInfo(ifStatement, false, output);

		depth += 2;
		output.append(ifStatement.getCondition().accept(this));
		output.append(ifStatement.getOperation().accept(this));
		if (ifStatement.hasElse())
			output.append(ifStatement.getElseOperation().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(While whileStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, whileStatement);
		output.append("While statement");
		appendTypeInfo(whileStatement, false, output);

		depth += 2;
		output.append(whileStatement.getCondition().accept(this));
		output.append(whileStatement.getOperation().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(Break breakStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, breakStatement);
		output.append("Break statement");
		appendTypeInfo(breakStatement, false, output);

		return output.toString();
	}

	public Object visit(Continue continueStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, continueStatement);
		output.append("Continue statement");
		appendTypeInfo(continueStatement, false, output);

		return output.toString();
	}

	public Object visit(StatementsBlock statementsBlock) {
		StringBuffer output = new StringBuffer();

		indent(output, statementsBlock);
		output.append("Block of statements");
		appendTypeInfo(statementsBlock, false, output);

		depth += 2;
		for (Statement statement : statementsBlock.getStatements())
			output.append(statement.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(LocalVariable localVariable) {
		StringBuffer output = new StringBuffer();

		indent(output, localVariable);
		output.append("Declaration of local variable: "
				+ localVariable.getName());
		if (localVariable.hasInitValue()) {
			output.append(", with initial value");
		}
		appendTypeInfo(localVariable, true, output);

		//output.append(localVariable.getType().accept(this));
		if (localVariable.hasInitValue()) {
			depth+=2;
			output.append(localVariable.getInitValue().accept(this));
			depth-=2;
		}
		return output.toString();
	}

	public Object visit(VariableLocation location) {
		StringBuffer output = new StringBuffer();

		indent(output, location);
		output.append("Reference to variable: " + location.getName());
		if (location.isExternal())
			output.append(", in external scope");
		appendTypeInfo(location, true, output);

		if (location.isExternal()) {
			depth+=2;
			output.append(location.getLocation().accept(this));
			depth-=2;
		}
		return output.toString();
	}

	public Object visit(ArrayLocation location) {
		StringBuffer output = new StringBuffer();

		indent(output, location);
		output.append("Reference to array");
		appendTypeInfo(location, true, output);

		depth += 2;
		output.append(location.getArray().accept(this));
		output.append(location.getIndex().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(StaticCall call) {
		StringBuffer output = new StringBuffer();

		indent(output, call);
		output.append("Call to static method: " + call.getName()
				+ ", in class " + call.getClassName());
		appendTypeInfo(call, false, output);

		depth += 2;
		for (Expression argument : call.getArguments())
			output.append(argument.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(VirtualCall call) {
		StringBuffer output = new StringBuffer();

		indent(output, call);
		output.append("Call to virtual method: " + call.getName());
		if (call.isExternal())
			output.append(", in external scope");
		appendTypeInfo(call, false, output);

		depth += 2;
		if (call.isExternal())
			output.append(call.getLocation().accept(this));
		for (Expression argument : call.getArguments())
			output.append(argument.accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(This thisExpression) {
		StringBuffer output = new StringBuffer();

		indent(output, thisExpression);
		output.append("Reference to 'this' instance");
		appendTypeInfo(thisExpression, false, output);

		return output.toString();
	}

	public Object visit(NewClass newClass) {
		StringBuffer output = new StringBuffer();

		indent(output, newClass);
		output.append("Instantiation of class: " + newClass.getName());
		appendTypeInfo(newClass, true, output);

		return output.toString();
	}

	public Object visit(NewArray newArray) {
		StringBuffer output = new StringBuffer();

		indent(output, newArray);
		output.append("Array allocation");
		appendTypeInfo(newArray, true, output);

		depth += 2;
		output.append(newArray.getType().accept(this));
		output.append(newArray.getSize().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(Length length) {
		StringBuffer output = new StringBuffer();

		indent(output, length);
		output.append("Reference to array length");
		appendTypeInfo(length, false, output);

		depth+=2;
		output.append(length.getArray().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(MathBinaryOp binaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, binaryOp);
		output.append("Mathematical binary operation: "
				+ binaryOp.getOperator().getDescription());
		appendTypeInfo(binaryOp, true, output);

		depth += 2;
		output.append(binaryOp.getFirstOperand().accept(this));
		output.append(binaryOp.getSecondOperand().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(LogicalBinaryOp binaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, binaryOp);
		output.append("Logical binary operation: "
				+ binaryOp.getOperator().getDescription());
		appendTypeInfo(binaryOp, true, output);

		depth += 2;
		output.append(binaryOp.getFirstOperand().accept(this));
		output.append(binaryOp.getSecondOperand().accept(this));
		depth -= 2;
		return output.toString();
	}

	public Object visit(MathUnaryOp unaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, unaryOp);
		output.append("Mathematical unary operation: "
				+ unaryOp.getOperator().getDescription());
		appendTypeInfo(unaryOp, true, output);

		depth+=2;
		output.append(unaryOp.getOperand().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(LogicalUnaryOp unaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, unaryOp);
		output.append("Logical unary operation: "
				+ unaryOp.getOperator().getDescription());
		appendTypeInfo(unaryOp, true, output);

		depth+=2;
		output.append(unaryOp.getOperand().accept(this));
		depth-=2;
		return output.toString();
	}

	public Object visit(Literal literal) {
		StringBuffer output = new StringBuffer();

		indent(output, literal);
		output.append(literal.getType().getDescription() + ": "
				+ literal.getType().toFormattedString(literal.getValue()));
		appendTypeInfo(literal, true, output);
		return output.toString();
	}

	public Object visit(ExpressionBlock expressionBlock) {
		StringBuffer output = new StringBuffer();

		indent(output, expressionBlock);
		output.append("Parenthesized expression");
		appendTypeInfo(expressionBlock, true, output);

		depth+=2;
		output.append(expressionBlock.getExpression().accept(this));
		depth-=2;
		return output.toString();
	}
}