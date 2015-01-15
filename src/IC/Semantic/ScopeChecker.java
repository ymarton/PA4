package IC.Semantic;

import IC.DataTypes;
import IC.AST.*;
import IC.Symbols.Kind;
import IC.Symbols.Property;
import IC.Symbols.Symbol;
import IC.Symbols.SymbolTable;
import IC.Types.AbstractEntryTypeTable;
import IC.Types.ClassTypeEntry;
import IC.Types.MethodTypeEntry;
import IC.Types.PrimitiveTypeEnum;
import IC.Types.TypesTable;

public class ScopeChecker implements ThrowingVisitor {

	private boolean seenMain = false;
	private boolean isStatic = false;
	private boolean isLoop = false;

	public boolean seenMain()
	{
		return this.seenMain;
	}
	
	public void staticON()
	{
		this.isStatic = true;
	}

	public void staticOFF()
	{
		this.isStatic = false;
	}

	public void loopON()
	{
		this.isLoop = true;
	}

	public void loopOFF()
	{
		this.isLoop = false;
	}

	@Override
	public Object visit(Program program) throws Exception {
		return program.getEnclosingScope().getSymbolByID("program");
	}

	@Override
	public Object visit(ICClass icClass) throws Exception{
		return icClass.getEnclosingScope().getSymbolByID(icClass.getName());
	}

	@Override
	public Object visit(Field field) throws Exception {
		if (isIDalreadyDeclaredAnyType(field.getName(), field.getEnclosingScope()))
			throw DuplicatedIdDError(field.getName(), field.getLine());

		Symbol fieldSymbol = field.getEnclosingScope().getSymbolByID(field.getName());
		fieldSymbol.markAsSeen();
		field.getEnclosingScope().AddSymbol(fieldSymbol);
		return fieldSymbol;
	}

	@Override
	public Object visit(VirtualMethod method) throws Exception {
		//allowing overriding exact same signature
		if (isMethodAlreadyDeclaredDifferentSig(method.getEnclosingScope(), method, Property.VIRTUAL))
			throw DuplicatedIdDError(method.getName(), method.getLine());

		return method.getEnclosingScope().getSymbolByID(method.getName());
	}

	@Override
	public Object visit(StaticMethod method) throws Exception {
		//allowing overriding exact same signature
		if (isMethodAlreadyDeclaredDifferentSig(method.getEnclosingScope(), method, Property.STATIC))
			throw DuplicatedIdDError(method.getName(), method.getLine());

		// check if its also main
		if (isStaticMethodAlsoMain(method))
		{
			// does it the one and only main
			if (this.seenMain)
				throw new SemanticError("duplicated main declaration", method.getLine());
			this.seenMain = true;
		}
		return method.getEnclosingScope().getSymbolByID(method.getName());
	}

	@Override
	public Object visit(LibraryMethod method) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");
	}

	@Override
	public Object visit(Formal formal) throws Exception {
		Symbol formalSymbol = formal.getEnclosingScope().getSymbolByID(formal.getName());
		formalSymbol.markAsSeen();
		formal.getEnclosingScope().AddSymbol(formalSymbol);
		return formalSymbol;
	}

	@Override
	public Object visit(PrimitiveType type) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");	
	}

	@Override
	public Object visit(UserType type) throws Exception {
		if (ClassesGraph.getClassNode(type.getName()) == null)
			throw UndeclaredUsageError(type.getName(), type.getLine());

		return TypesTable.getTypeEntryForType(type);
	}

	@Override
	public Object visit(Assignment assignment) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");
	}

	@Override
	public Object visit(CallStatement callStatement) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(Return returnStatement) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(If ifStatement) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(While whileStatement) throws Exception {
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(Break breakStatement) throws Exception {
		if (!isLoop)
			throw NotInLoopError("BREAK", breakStatement.getLine());
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) throws Exception {
		if (!isLoop)
			throw NotInLoopError("CONTINUE", continueStatement.getLine());
		return null;
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(LocalVariable localVariable) throws Exception{
		Symbol localSymbol = localVariable.getEnclosingScope().getSymbolByID(localVariable.getName());
		localSymbol.markAsSeen();
		localVariable.getEnclosingScope().AddSymbol(localSymbol); //2612 now marked
		return localSymbol;
	}

	@Override
	public Object visit(VariableLocation location) throws Exception{

		String ID = location.getName();

		// location == ID 
		if (!location.isExternal())
		{
			if (isStatic)
			{
				// static? then it can be a local/formal var declared on superclass/enclosing block (no field!)
				SymbolTable currentScope = location.getEnclosingScope();
				while (currentScope != null)
				{
					if (currentScope.isIDDeclaredAndSeenVarFormalType(ID))
						return currentScope.getSymbolByID(ID);
					currentScope = currentScope.getParentTable();
				}
				throw UndeclaredUsageError(ID, location.getLine());
			}
			// no static? then it can be a field/local/formal var declared on superclass/enclosing block
			SymbolTable currentScope = location.getEnclosingScope();
			while (currentScope != null)
			{
				if (currentScope.isIDDeclaredAndSeenVarFormalFieldType(ID))
					return currentScope.getSymbolByID(ID);
				currentScope = currentScope.getParentTable();
			}
			throw UndeclaredUsageError(ID, location.getLine());
		}
		// location == expr.ID, external scope
		else
		{
			AbstractEntryTypeTable expType = location.getLocation().getAssignedType();

			if (!(expType instanceof ClassTypeEntry))
				throw new SemanticError("field referencing isn't allowed for types different than class instance", location.getLine());

			// search the ID (field) in the class scope
			String className = ((ClassTypeEntry)expType).getName();
			SymbolTable currentScope = SymbolTable.getTopTable().getSymbolByID(className).getSymTableRef();

			while (currentScope != null)
			{
				if (currentScope.isIDDeclaredAndSeenVarFormalFieldType(ID))
					return currentScope.getSymbolByID(ID);
				currentScope = currentScope.getParentTable();
			}
			throw UndeclaredUsageError(ID, location.getLine());
		}
	}

	@Override
	public Object visit(ArrayLocation location) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(StaticCall call) throws Exception{
		String className = call.getClassName();
		ClassNode classNode = ClassesGraph.getClassNode(className);
		if (classNode == null)
			throw UndeclaredUsageError(className, call.getLine());

		SymbolTable currentScope = SymbolTable.getTopTable().getSymbolByID(className).getSymTableRef();
		String methodName = call.getName();
		while (currentScope != null)
		{
			if (currentScope.isDeclaredMethod(methodName, Property.STATIC))
				return currentScope.getSymbolByID(methodName);
			currentScope = currentScope.getParentTable();
		}
		throw UndeclaredUsageError(methodName, call.getLine());
	}

	@Override
	public Object visit(VirtualCall call) throws Exception{
		SemanticError scopeError = new SemanticError("Virtual func. can't be invoked inside static methods", call.getLine());

		if (isStatic)
			throw scopeError;
		
		SymbolTable currentScope = call.getEnclosingScope();
		if (call.isExternal())
		{
			String className = ((ClassTypeEntry)call.getLocation().getAssignedType()).getName();
			ClassNode classNode = ClassesGraph.getClassNode(className);
			if (classNode == null)
				throw UndeclaredUsageError(className, call.getLine());

			currentScope = SymbolTable.getTopTable().getSymbolByID(className).getSymTableRef();
		}
		String methodName = call.getName();

		while (currentScope != null)
		{
			if (currentScope.isDeclaredMethod(methodName, Property.VIRTUAL))
				return currentScope.getSymbolByID(methodName);
			currentScope = currentScope.getParentTable();
		}
		throw UndeclaredUsageError(methodName, call.getLine());
	}

	@Override
	public Object visit(This thisExpression) throws Exception{
		SemanticError scopeError = new SemanticError("THIS expression can only be used inside virtual methods", thisExpression.getLine());

		if (isStatic)
			throw scopeError;

		String currentScopeID = thisExpression.getEnclosingScope().getID();
		SymbolTable parentScope = thisExpression.getEnclosingScope().getParentTable();

		while (parentScope != null)
		{
			Symbol scopeOwner = parentScope.getSymbolByID(currentScopeID);
			if ((scopeOwner != null) && (scopeOwner.getKind() == Kind.METHOD))
			{
				// parentScope.id == class name that its child table contains the method
				return  SymbolTable.getTopTable().getSymbolByID(parentScope.getID());
			}
			// for StatementBlock, stmtBlocksymbol.id  != stmtBlocksymbol.childSymbolTable.id
			parentScope = parentScope.getParentTable();
		}

		throw scopeError;
	}

	@Override
	public Object visit(NewClass newClass) throws Exception{
		String className = newClass.getName();
		ClassNode classNode = ClassesGraph.getClassNode(className);
		if (classNode == null)
			throw UndeclaredUsageError(className, newClass.getLine());

		return SymbolTable.getTopTable().getSymbolByID(className);
	}

	@Override
	public Object visit(NewArray newArray) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(Length length) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(MathBinaryOp binaryOp) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(MathUnaryOp unaryOp) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");
	}

	@Override
	public Object visit(Literal literal) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) throws Exception{
		throw new Exception("not implemented! unexpected scope check visit invocation");

	}

	// is declared on this or super scope, no limits
	private static boolean isIDalreadyDeclaredAnyType(String nodeID, SymbolTable nodeScope)
	{
		SymbolTable currentScope = nodeScope.getParentTable();
		while (currentScope != null)
		{
			if (!currentScope.isFirstDecleration(nodeID))
				return true;
			currentScope = currentScope.getParentTable();
		}
		return false;
	}

	//used to the question - can we declare the method? no other id with method id, same id allowed only if it will be method overriding (inc. static/virtual!)
	private static boolean isMethodAlreadyDeclaredDifferentSig(SymbolTable currentScope, Method method, Property methodProperty)
	{
		//starting from parent
		currentScope = currentScope.getParentTable();
		while (currentScope != null)
		{
			Symbol symbolFound = currentScope.getSymbolByID(method.getName());
			if (symbolFound != null)
			{
				if (symbolFound.getKind() != Kind.METHOD)
					return true;
				if (symbolFound.getExtraProperty() != methodProperty)
					return true;
				if (! (symbolFound.getTypeEntry().equals(new MethodTypeEntry(method.getType(), method.getFormals(), 0))) )
					return true;
				return false;
			}
			currentScope = currentScope.getParentTable();
		}
		return false;
	}
	
	private static boolean isStaticMethodAlsoMain(Method method)
	{
		if (!(method.getName().equals("main")))
			return false;
		AbstractEntryTypeTable voidEntryType = TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.VOID);
		AbstractEntryTypeTable methodEntryType = TypesTable.getTypeEntryForType(method.getType());
		if (!(methodEntryType.equals(voidEntryType)))
			return false;
		
		if (method.getFormals().size() != 1)
			return false;
		
		Type args = new PrimitiveType(0, DataTypes.STRING);
		args.incrementDimension();
		AbstractEntryTypeTable argsEntryType = TypesTable.getTypeEntryForType(args);
		AbstractEntryTypeTable paramEntryType = TypesTable.getTypeEntryForType(method.getFormals().get(0).getType());
		if (!(paramEntryType.equals(argsEntryType)))
			return false;
		return true;
	}
	
	private static SemanticError NotInLoopError(String stmtDesc, int line)
	{
		return new SemanticError(stmtDesc + " statement can appear only inside loop", line);
	}
	private static SemanticError DuplicatedIdDError(String ID, int line)
	{
		return new SemanticError("already declared id: " + ID, line);
	}

	private static SemanticError UndeclaredUsageError(String ID, int line)
	{
		return new SemanticError("usage of undeclared symbol: " + ID, line);
	}
}
