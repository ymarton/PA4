package IC.Semantic;

import IC.AST.*;
import IC.LiteralTypes;
import IC.Symbols.Symbol;
import IC.Types.AbstractEntryTypeTable;
import IC.Types.ArrayTypeEntry;
import IC.Types.ClassTypeEntry;
import IC.Types.MethodTypeEntry;
import IC.Types.PrimitiveTypeEntry;
import IC.Types.PrimitiveTypeEnum;
import IC.Types.TypesTable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TypeChecking implements ThrowingVisitor {

	private boolean currentMethodHasReturn;
	private AbstractEntryTypeTable currentMethodReturnType;

	private ScopeChecker scopeChecker = new ScopeChecker();

	/**
	 * Type checking of a program
	 * @param program
	 * @return null
	 * @throws Exception
	 */
	@Override
	public Object visit(Program program) throws Exception {
		for (ICClass classDecl : program.getClasses()) {
			classDecl.accept(this);
		}
		
		if (!scopeChecker.seenMain())
			throw new SemanticError("missing main declaration", 0);
		return null;
	}

	/**
	 * Type checking of a class
	 * @param icClass
	 * @return icClass's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(ICClass icClass) throws Exception {
		for (Field field : icClass.getFields()) {
			field.accept(this);
		}

		for (Method method : icClass.getMethods()) {
			method.accept(this);
		}
		Symbol classSymbol = (Symbol) icClass.accept(scopeChecker);
		icClass.setAssignedType(classSymbol.getTypeEntry());
		return icClass.getAssignedType();
	}

	/**
	 * Type checking of a field
	 * @param field
	 * @return field's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(Field field) throws Exception {
		AbstractEntryTypeTable fieldType =  (AbstractEntryTypeTable) field.getType().accept(this);
		field.accept(scopeChecker);
		field.setAssignedType(fieldType);
		return fieldType;
	}

	/**
	 * Type checking of a virtual method
	 * @param method
	 * @return method's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(VirtualMethod method) throws Exception {
		this.currentMethodReturnType = (AbstractEntryTypeTable) method.getType().accept(this);
		this.currentMethodHasReturn = false;
		PrimitiveTypeEntry voidType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.VOID);
		if (this.currentMethodReturnType.equals(voidType))
			this.currentMethodHasReturn = true;
		for (Formal formal : method.getFormals())
			formal.accept(this);
		for (Statement statement : method.getStatements())
			statement.accept(this);
		if (!this.currentMethodHasReturn)
			throw new SemanticError("No return statement in non void method", method.getLine());

		Symbol methodSymbol  = (Symbol) method.accept(scopeChecker);
		method.setAssignedType(methodSymbol.getTypeEntry());
		return method.getAssignedType();
	}

	/**
	 * Type checking of a static method
	 * @param method
	 * @return method's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(StaticMethod method) throws Exception {
		scopeChecker.staticON();
		this.currentMethodReturnType = (AbstractEntryTypeTable) method.getType().accept(this);
		this.currentMethodHasReturn = false;
		PrimitiveTypeEntry voidType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.VOID);
		if (this.currentMethodReturnType.equals(voidType))
			this.currentMethodHasReturn = true;
		for (Formal formal : method.getFormals())
			formal.accept(this);
		for (Statement statement : method.getStatements())
			statement.accept(this);
		if (!this.currentMethodHasReturn)
			throw new SemanticError("No return statement in non void method", method.getLine());
		Symbol methodSymbol = (Symbol) method.accept(scopeChecker);
		method.setAssignedType(methodSymbol.getTypeEntry());
		scopeChecker.staticOFF();
		return method.getAssignedType();
	}

	/**
	 * Type checking of a Library method
	 * @param method
	 * @return method's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(LibraryMethod method) throws Exception{
		AbstractEntryTypeTable methodType = (AbstractEntryTypeTable) method.getType().accept(this);
		for (Formal formal : method.getFormals())
			formal.accept(this);
		MethodTypeEntry methodTypeEntry = new MethodTypeEntry(method.getType(), method.getFormals(), 0);
		method.setAssignedType(methodTypeEntry);
		return methodType;
	}

	/**
	 * Type checking of a formal
	 * @param formal
	 * @return formal's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(Formal formal) throws Exception {
		AbstractEntryTypeTable formalType = (AbstractEntryTypeTable) formal.getType().accept(this);
		formal.accept(scopeChecker);
		formal.setAssignedType(formalType);
		return formalType;
	}

	/**
	 * Type checking of a primitive type
	 * @param type
	 * @return type's evaluated type
	 */
	@Override
	public Object visit(PrimitiveType type) {
		AbstractEntryTypeTable primitiveType = TypesTable.getTypeEntryForType(type);
		type.setAssignedType(primitiveType);
		return primitiveType;
	}

	/**
	 * Type checking of a user type
	 * @param type
	 * @return type's evaluated type
	 */
	@Override
	public Object visit(UserType type) {
		AbstractEntryTypeTable userType = (AbstractEntryTypeTable) type.accept(scopeChecker);
		type.setAssignedType(userType);
		return userType;
	}

	/**
	 * Type checking of an assignment statement
	 * @param assignment
	 * @return variable's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(Assignment assignment) throws Exception {
		AbstractEntryTypeTable a = (AbstractEntryTypeTable) assignment.getVariable().accept(this);
		AbstractEntryTypeTable b = (AbstractEntryTypeTable) assignment.getAssignment().accept(this);
		compareTypes(a, b, assignment.getLine());
		return a;
	}

	/**
	 * Type checking of a call statement
	 * @param callStatement - the ASTNode of the statement
	 * @return call's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(CallStatement callStatement) throws Exception {
		return callStatement.getCall().accept(this);
	}

	/**
	 * Type checking of a return statement
	 * @param returnStatement - the ASTNode of the statement
	 * @return return's value evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(Return returnStatement) throws Exception {
		PrimitiveTypeEntry voidType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.VOID);
		PrimitiveTypeEntry stringType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING);
		PrimitiveTypeEntry nullType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.NULL);
		if (returnStatement.hasValue()) { //return somthing;
			if (voidType.equals(this.currentMethodReturnType))
				throw new SemanticError("Returning a non-void type in a void-type function", returnStatement.getLine());
			AbstractEntryTypeTable returnType = (AbstractEntryTypeTable) returnStatement.getValue().accept(this);
			compareTypes(this.currentMethodReturnType, returnType, returnStatement.getLine());
			this.currentMethodHasReturn = true;
			return this.currentMethodReturnType;
		}
		else { // "return;" check the method returns void
			if (voidType.equals(this.currentMethodReturnType)) {
				this.currentMethodHasReturn = true;
				return this.currentMethodReturnType;
			}
			else
				throw new SemanticError("Missing return statement value", returnStatement.getLine());
		}
	}

	/**
	 * Type checking of an if statement
	 * @param ifStatement - the ASTNode of the statement
	 * @return null
	 * @throws Exception
	 */
	@Override
	public Object visit(If ifStatement) throws Exception {
		AbstractEntryTypeTable conditionTypeEntry = (AbstractEntryTypeTable) ifStatement.getCondition().accept(this);
		PrimitiveTypeEntry booleanTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
		if (!conditionTypeEntry.equals(booleanTypeEntry))
			throw new SemanticError("Non boolean condition for if statement", ifStatement.getLine());
		ifStatement.getOperation().accept(this);
		if (ifStatement.hasElse())
			ifStatement.getElseOperation().accept(this);
		return null;
	}

	/**
	 * Type checking of a while statement
	 * @param whileStatement - the ASTNode of the statement
	 * @return null
	 * @throws Exception
	 */
	@Override
	public Object visit(While whileStatement) throws Exception {
		String GUID = UUID.randomUUID().toString();
		scopeChecker.loopON(GUID);
		AbstractEntryTypeTable conditionTypeEntry = (AbstractEntryTypeTable) whileStatement.getCondition().accept(this);
		PrimitiveTypeEntry booleanTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
		if (!conditionTypeEntry.equals(booleanTypeEntry))
			throw new SemanticError("Non boolean condition for while statement", whileStatement.getLine());
		if (whileStatement.getOperation() != null) {
			whileStatement.getOperation().accept(this);
		}
		scopeChecker.loopOFF(GUID);
		return null;
	}

	/**
	 * Type checking of a break statement
	 * @param breakStatement - the ASTNode of the statement
	 * @return null
	 */
	@Override
	public Object visit(Break breakStatement) {
		breakStatement.accept(scopeChecker);
		return null;
	}

	/**
	 * Type checking of a continue statement
	 * @param continueStatement - the ASTNode of the statement
	 * @return null
	 */
	@Override
	public Object visit(Continue continueStatement) {
		continueStatement.accept(scopeChecker); //2612 only in loop
		return null;
	}

	/**
	 * Type checking of a statement block
	 * @param statementsBlock - the ASTNode of the block
	 * @return null
	 * @throws Exception
	 */
	@Override
	public Object visit(StatementsBlock statementsBlock) throws Exception {
		for (Statement s : statementsBlock.getStatements()) {
			s.accept(this);
		}
		return null;
	}

	/**
	 * Type checking of a local variable statement
	 * @param localVariable - the ASTNode of the statement
	 * @return variable's evaluated type definition
	 * @throws Exception
	 */
	@Override
	public Object visit(LocalVariable localVariable) throws Exception {
		Type type = localVariable.getType();
		AbstractEntryTypeTable localVariableType = (AbstractEntryTypeTable) type.accept(this);
		localVariable.accept(scopeChecker);
		if (localVariable.hasInitValue()) {
			AbstractEntryTypeTable initValueType = (AbstractEntryTypeTable)localVariable.getInitValue().accept(this);
			compareTypes(localVariableType, initValueType, localVariable.getLine());
			localVariable.setAssignedType(localVariableType);
			return localVariableType;
		}
		localVariable.setAssignedType(localVariableType);
		return localVariableType;
	}

	/**
	 * Type checking of a variable reference
	 * @param location - the ASTNode of the reference
	 * @return variable's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(VariableLocation location) throws Exception {
		if (location.isExternal()) {
			location.getLocation().accept(this);
		}
		Symbol idSymbol = (Symbol)location.accept(scopeChecker);
		location.setAssignedType(idSymbol.getTypeEntry());
		return idSymbol.getTypeEntry();
	}

	/**
	 * Type checking of an array's reference
	 * @param location - the ASTNode of the reference
	 * @return array's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(ArrayLocation location) throws Exception {
		AbstractEntryTypeTable indexType = (AbstractEntryTypeTable) location.getIndex().accept(this);
		AbstractEntryTypeTable intPrimitiveEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
		if (!indexType.equals(intPrimitiveEntry))
			throw new SemanticError("Index should be integer", location.getLine());
		AbstractEntryTypeTable arrayType = (AbstractEntryTypeTable) location.getArray().accept(this);
		if (!(arrayType instanceof ArrayTypeEntry))
			throw new SemanticError("Invalid array expression", location.getLine());
		else {
			Type arrayTypeInternal = ((ArrayTypeEntry)arrayType).getType();
			Type locationTypeInternal;
			if (arrayTypeInternal instanceof PrimitiveType)
				locationTypeInternal = PrimitiveType.cloneTypeDecreaseOneDim((PrimitiveType)arrayTypeInternal);
			else // instanceof UserType
				locationTypeInternal = UserType.cloneTypeDecreaseOneDim((UserType)arrayTypeInternal);
			AbstractEntryTypeTable locationType = TypesTable.getTypeEntryForType(locationTypeInternal);
			location.setAssignedType(locationType);
			return locationType;
		}
	}

	/**
	 * Type checking of a static call
	 * @param call - the ASTNode of the call
	 * @return call's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(StaticCall call) throws Exception {
		// call = C.method
		Symbol method = (Symbol) call.accept(scopeChecker);
		AbstractEntryTypeTable methodTypeEntry = method.getTypeEntry();
		List<Type> params = ((MethodTypeEntry)methodTypeEntry).getParams();
		if (call.getArguments().size() != params.size())
			throw new SemanticError("Incompatible number of arguments in a call to the function " + call.getName(), call.getLine());
		for (Expression arg : call.getArguments())
			arg.accept(this);
		Iterator<Type> paramsIterator = params.iterator();
		Iterator<Expression> argsIterator = call.getArguments().iterator();
		while (paramsIterator.hasNext() && argsIterator.hasNext())
			compareTypes(TypesTable.getTypeEntryForType(paramsIterator.next()), argsIterator.next().getAssignedType(), call.getLine());
		AbstractEntryTypeTable returnType = TypesTable.getTypeEntryForType(((MethodTypeEntry) methodTypeEntry).getRetType());
		call.setAssignedType(returnType);
		return returnType;
	}

	/**
	 * Type checking of a virtual call
	 * @param call - the ASTNode of the call
	 * @return call's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(VirtualCall call) throws Exception {
		if (call.isExternal()) {
			call.getLocation().accept(this);
		}
		Symbol method = (Symbol) call.accept(scopeChecker);
		AbstractEntryTypeTable methodTypeEntry = method.getTypeEntry();
		List<Type> params = ((MethodTypeEntry)methodTypeEntry).getParams();
		if (call.getArguments().size() != params.size())
			throw new SemanticError("Incompatible number of arguments in a call to the function " + call.getName(), call.getLine());
		for (Expression arg : call.getArguments())
			arg.accept(this);
		Iterator<Type> paramsIterator = params.iterator();
		Iterator<Expression> argsIterator = call.getArguments().iterator();
		while (paramsIterator.hasNext() && argsIterator.hasNext())
			compareTypes(TypesTable.getTypeEntryForType(paramsIterator.next()), argsIterator.next().getAssignedType(), call.getLine());
		AbstractEntryTypeTable returnType = TypesTable.getTypeEntryForType(((MethodTypeEntry) methodTypeEntry).getRetType());
		call.setAssignedType(returnType);
		return returnType;
	}

	/**
	 * Type checking of a this statement
	 * @param thisExpression - the ASTNode of the statement
	 * @return this evaluated type
	 */
	@Override
	public Object visit(This thisExpression) {
		Symbol thisSymbol = (Symbol)thisExpression.accept(scopeChecker);
		AbstractEntryTypeTable thisType = thisSymbol.getTypeEntry();
		thisExpression.setAssignedType(thisType);
		return thisType;
	}

	/**
	 * Type checking of a new class statement
	 * @param newClass - the ASTNode of the statement
	 * @return the class's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(NewClass newClass) throws Exception {
		Symbol classSymbol = (Symbol) newClass.accept(scopeChecker);
		newClass.setAssignedType(classSymbol.getTypeEntry());
		return classSymbol.getTypeEntry();
	}

	/**
	 * Type checking of a new array statement
	 * @param newArray - the ASTNode of the statement
	 * @return the array's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(NewArray newArray) throws Exception{
		AbstractEntryTypeTable sizeType = (AbstractEntryTypeTable) newArray.getSize().accept(this);
		PrimitiveTypeEntry intType = (PrimitiveTypeEntry) TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
		if (!sizeType.equals(intType))
			throw new SemanticError("Size of array should be int", newArray.getLine());
		else {
			//2712
			Type arrayT = newArray.getType();
			arrayT.incrementDimension();
			AbstractEntryTypeTable arrayType = (AbstractEntryTypeTable)arrayT.accept(this);
			newArray.setAssignedType(arrayType);
			return arrayType;
		}
	}

	/**
	 * Type checking of length expression
	 * @param length - the ASTNode of the expression
	 * @return int primitive type
	 * @throws Exception
	 */
	@Override
	public Object visit(Length length) throws Exception {
		AbstractEntryTypeTable exprType = (AbstractEntryTypeTable)length.getArray().accept(this);
		if (exprType instanceof ArrayTypeEntry) {
			PrimitiveTypeEntry intType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
			length.setAssignedType(intType);
			return intType;
		}
		else
			throw new SemanticError("Calling 'length' on a non-array type", length.getLine());
	}

	/**
	 * Type checking of a mathematical binary expression
	 * @param binaryOp - the ASTNode of the expression
	 * @return int primitive type for two ints or string primitive type for two strings addition
	 * @throws Exception
	 */
	@Override
	public Object visit(MathBinaryOp binaryOp) throws Exception{
		AbstractEntryTypeTable a = (AbstractEntryTypeTable) binaryOp.getFirstOperand().accept(this);
		AbstractEntryTypeTable b = (AbstractEntryTypeTable) binaryOp.getSecondOperand().accept(this);
		PrimitiveTypeEntry intTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
		PrimitiveTypeEntry stringTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING);
		String op = null;
		switch (binaryOp.getOperator()) {
		case PLUS:
			if (a.equals(intTypeEntry) && b.equals(intTypeEntry)) {
				binaryOp.setAssignedType(intTypeEntry);
				return intTypeEntry;
			}
			if (a.equals(stringTypeEntry) && b.equals(stringTypeEntry)) {
				binaryOp.setAssignedType(stringTypeEntry);
				return stringTypeEntry;
			}
			throw new SemanticError("Type mismatch: can't add " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), binaryOp.getLine());
		case MINUS:
			op = "substract";
		case MULTIPLY:
			if (op == null)
				op = "multiply";
		case DIVIDE:
			if (op == null)
				op = "divide";
		case MOD:
			if (op == null)
				op = "modulo";
			if (a.equals(intTypeEntry) && b.equals(intTypeEntry)) {
				binaryOp.setAssignedType(intTypeEntry);
				return intTypeEntry;
			}
			else
				throw new SemanticError("Type mismatch: can't " + op + " " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), binaryOp.getLine());
		default:
			throw new Exception("we shouldn't reach here - grammer problem"); //2612 warning

		}
	}

	/**
	 * Type checking of a logical binary expression
	 * @param binaryOp - the ASTNode of the expression
	 * @return boolean primitive type
	 * @throws Exception
	 */
	@Override
	public Object visit(LogicalBinaryOp binaryOp) throws Exception {
		AbstractEntryTypeTable a = (AbstractEntryTypeTable) binaryOp.getFirstOperand().accept(this);
		AbstractEntryTypeTable b = (AbstractEntryTypeTable) binaryOp.getSecondOperand().accept(this);
		PrimitiveTypeEntry booleanTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
		PrimitiveTypeEntry stringTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING);
		PrimitiveTypeEntry intTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
		switch (binaryOp.getOperator()) {
		case LAND:
		case LOR:
			if (a.equals(booleanTypeEntry) && b.equals(booleanTypeEntry)) {
				binaryOp.setAssignedType(a);
				return a;
			}
			else
				throw new SemanticError("Type mismatch: " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), binaryOp.getLine());
		case LT:
		case LTE:
		case GT:
		case GTE:
			if (a.equals(intTypeEntry) && b.equals(intTypeEntry)) {
				binaryOp.setAssignedType(booleanTypeEntry);
				return booleanTypeEntry;
			}
			else
				throw new SemanticError("Type mismatch: " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), binaryOp.getLine());
		case EQUAL:
		case NEQUAL:
			PrimitiveTypeEntry nullTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.NULL);
			if (a.equals(b)) {
				binaryOp.setAssignedType(booleanTypeEntry);
				return booleanTypeEntry;
			}
			if ((a.equals(nullTypeEntry) && b instanceof ClassTypeEntry) || (a instanceof ClassTypeEntry && b.equals(nullTypeEntry))) {
				binaryOp.setAssignedType(booleanTypeEntry);
				return booleanTypeEntry;
			}
			if (b instanceof ClassTypeEntry && a instanceof ClassTypeEntry) {
				String aName = ((ClassTypeEntry) a).getName();
				String bName = ((ClassTypeEntry) b).getName();
				if (ClassesGraph.isDerivedAndSuper(bName, aName) || ClassesGraph.isDerivedAndSuper(aName, bName)) {
					binaryOp.setAssignedType(booleanTypeEntry);
					return booleanTypeEntry;
				}
			}
			if ((a.equals(nullTypeEntry) && b.equals(stringTypeEntry)) || (a.equals(stringTypeEntry) && b.equals(nullTypeEntry))) {
				binaryOp.setAssignedType(booleanTypeEntry);
				return booleanTypeEntry;
			}
			throw new SemanticError("Type mismatch: " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), binaryOp.getLine());
		default:
			throw new Exception("we shouldn't reach here - grammer problem"); //2612 warning
		}

	}

	/**
	 * Type checking of a mathematical unary expression
	 * @param unaryOp - the ASTNode of the expression
	 * @return integer primitive type
	 * @throws Exception
	 */
	@Override
	public Object visit(MathUnaryOp unaryOp) throws Exception {
		AbstractEntryTypeTable operandType = (AbstractEntryTypeTable) unaryOp.getOperand().accept(this);
		PrimitiveTypeEntry intTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
		if (operandType.equals(intTypeEntry)) {
			unaryOp.setAssignedType(intTypeEntry);
			return intTypeEntry;
		}
		else
			throw new SemanticError("Type mismatch", unaryOp.getLine());
	}

	/**
	 * Type checking of a logical unary expression
	 * @param unaryOp - the ASTNode of the expression
	 * @return boolean primitive type
	 * @throws Exception
	 */
	@Override
	public Object visit(LogicalUnaryOp unaryOp) throws Exception {
		AbstractEntryTypeTable operandType = (AbstractEntryTypeTable) unaryOp.getOperand().accept(this);
		PrimitiveTypeEntry booleanTypeEntry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
		if (operandType.equals(booleanTypeEntry)) {
			unaryOp.setAssignedType(booleanTypeEntry);
			return booleanTypeEntry;
		}
		else
			throw new SemanticError("Type mismatch", unaryOp.getLine());
	}

	/**
	 * Type checking of a literal expression
	 * @param literal - the ASTNode of the literal
	 * @return the literal's primitive type
	 */
	@Override
	public Object visit(Literal literal) {
		LiteralTypes literalType = literal.getType();
		PrimitiveTypeEntry entry = null;
		switch (literalType) {
		case TRUE:
			entry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
			literal.setAssignedType(entry);
			break;
		case FALSE:
			entry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.BOOLEAN);
			literal.setAssignedType(entry);
			break;
		case INTEGER:
			entry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.INT);
			literal.setAssignedType(entry);
			break;
		case STRING:
			entry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING);
			literal.setAssignedType(entry);
			break;
		case NULL:
			entry = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.NULL);
			literal.setAssignedType(entry);
		}
		return entry;
	}

	/**
	 * Type checking of an expression block
	 * @param expressionBlock - the ASTNode of the block
	 * @return the block's evaluated type
	 * @throws Exception
	 */
	@Override
	public Object visit(ExpressionBlock expressionBlock) throws Exception {
		AbstractEntryTypeTable type = (AbstractEntryTypeTable) expressionBlock.getExpression().accept(this);
		expressionBlock.setAssignedType(type);
		return type;
	}

	/**
	 *
	 * @param a - first type to compare
	 * @param b - second type to compare
	 * @param line - line number where the comparison is needed
	 * @throws Exception
	 */
	private void compareTypes(AbstractEntryTypeTable a, AbstractEntryTypeTable b, int line) throws Exception {
		if (a.equals(b))
			return; //same type (holds for all type cases (primitive, class, array))
		if (a instanceof ClassTypeEntry && b instanceof ClassTypeEntry) {
			String aName = ((ClassTypeEntry) a).getName();
			String bName = ((ClassTypeEntry) b).getName();
			if (ClassesGraph.isDerivedAndSuper(bName, aName))
				return; //b inherits from a
		}
		PrimitiveTypeEntry nullType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.NULL);
		PrimitiveTypeEntry stringType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING);
		if ((a.equals(stringType) || a instanceof ClassTypeEntry || a instanceof ArrayTypeEntry) && b.equals(nullType))
			return; //null assignment to a string, class or array
		throw new SemanticError("Incompatible types: " + a.getTypeDescrClean() + " and " + b.getTypeDescrClean(), line);
	}
}
