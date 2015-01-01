package IC.Symbols;
import IC.AST.*;
import IC.Semantic.ClassNode;
import IC.Semantic.ClassesGraph;
import IC.Semantic.SemanticError;
import IC.Types.*;

/**
 * builds symbol table AND types table!
 *
 */
public class SymbolTableBuilder implements PropagatingVisitor<SymbolTable, Symbol>
{

	@Override
	public Symbol visit(Program program, SymbolTable context) throws Exception
	{
		Symbol programSym = new Symbol("program", Kind.PROGRAM, null, true);
		SymbolTable scopeSelfContext = programSym.getSymTableRef();

		for (ICClass classDecl : program.getClasses())
		{
			Symbol classSymInclSubtables = classDecl.accept(this, scopeSelfContext);
			scopeSelfContext.AddSymbol(classSymInclSubtables);
		}

		SymbolTable.setTopTable(scopeSelfContext);
		return programSym;
	}

	@Override
	public Symbol visit(ICClass icClass, SymbolTable context) throws Exception
	{
		if (!context.isFirstDecleration(icClass.getName()))
			throw new SemanticError("duplicate class decleration: " + icClass.getName(), icClass.getLine());
		else
		{
			icClass.setEnclosingScope(context);

			ClassTypeEntry classType = new ClassTypeEntry(icClass.getName(), TypesTable.getCounter(), icClass.getSuperClassName());
			TypesTable.addClassEntry(classType);

			Symbol classSym = new Symbol(icClass.getName(), Kind.CLASS, classType, true);
			SymbolTable scopeSelfContext = classSym.getSymTableRef();
			scopeSelfContext.attachParentTable(context);

			for (Field field : icClass.getFields())
			{
				Symbol fieldSym = field.accept(this, scopeSelfContext);
				scopeSelfContext.AddSymbol(fieldSym);
			}

			for (Method method : icClass.getMethods())
			{
				Symbol methodSym = method.accept(this, scopeSelfContext);
				scopeSelfContext.AddSymbol(methodSym);
			}

			// classes graph section
			String className = icClass.getName();
			String superName = icClass.getSuperClassName();
			if (superName != null)
			{
				ClassesGraph.addVertex(new ClassNode(className, superName));
				ClassesGraph.addSuperDerivedRelation(superName, className);
			}
			else
			{
				ClassesGraph.addVertex(new ClassNode(className));
			}
			
			return classSym;
		}

	}

	@Override
	public Symbol visit(Field field, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(field.getName()))
			throw new SemanticError("duplicated field decleration: " + field.getName(), field.getLine());
		else
		{
			field.setEnclosingScope(context);
			field.getType().accept(this, context);

			AbstractEntryTypeTable fieldEntry = TypesTable.addSubDimsAndGetMyTypeEntry(field.getType());
			Symbol fieldSym = new Symbol(field.getName(), Kind.FIELD_ATTRIBUTE, fieldEntry, false);

			return fieldSym;
		}
	}

	@Override
	public Symbol visit(VirtualMethod method, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(method.getName()))
			throw new SemanticError("duplicated method decleration: " + method.getName(), method.getLine());
		else
		{
			method.setEnclosingScope(context);
			method.getType().accept(this, context);

			MethodTypeEntry methodEntry = null; // we have to create it after the creation of the formals' & retType entries
			Symbol methodSym = new Symbol(method.getName(), Kind.METHOD, methodEntry, true, Property.VIRTUAL);
			SymbolTable scopeSelfContext = methodSym.getSymTableRef();
			scopeSelfContext.attachParentTable(context);

			for (Formal formal : method.getFormals())
			{
				Symbol formalSym = formal.accept(this, scopeSelfContext);
				scopeSelfContext.AddSymbol(formalSym);
			}

			methodEntry = new MethodTypeEntry(method.getType(), method.getFormals(), TypesTable.getCounter());
			methodSym.updateType(methodEntry);
			TypesTable.addMethodEntry(methodEntry);

			for (Statement stmt : method.getStatements())
			{
				Symbol stmtSymbol = stmt.accept(this, scopeSelfContext);
				if ((stmtSymbol != null) && ((stmtSymbol.getKind() == Kind.VAR_LOCAL) || (stmtSymbol.getKind() == Kind.STMTS_BLOCK)))
					scopeSelfContext.AddSymbol(stmtSymbol);
			}

			return methodSym;
		}
	}

	@Override
	public Symbol visit(StaticMethod method, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(method.getName()))
			throw new SemanticError("duplicated method decleration: " + method.getName(), method.getLine());
		else
		{
			method.setEnclosingScope(context);
			method.getType().accept(this, context);

			MethodTypeEntry methodEntry = null;
			Symbol methodSym = new Symbol(method.getName(), Kind.METHOD, methodEntry, true, Property.STATIC);
			SymbolTable scopeSelfContext = methodSym.getSymTableRef();
			scopeSelfContext.attachParentTable(context);

			for (Formal formal : method.getFormals())
			{
				Symbol formalSym = formal.accept(this, scopeSelfContext);
				scopeSelfContext.AddSymbol(formalSym);
			}

			methodEntry = new MethodTypeEntry(method.getType(), method.getFormals(), TypesTable.getCounter());
			methodSym.updateType(methodEntry);
			TypesTable.addMethodEntry(methodEntry);

			for (Statement stmt : method.getStatements())
			{
				Symbol stmtSymbol = stmt.accept(this, scopeSelfContext);
				if ((stmtSymbol != null) && ((stmtSymbol.getKind() == Kind.VAR_LOCAL) || (stmtSymbol.getKind() == Kind.STMTS_BLOCK)))
					scopeSelfContext.AddSymbol(stmtSymbol);
			}

			return methodSym;
		}
	}

	@Override
	public Symbol visit(LibraryMethod method, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(method.getName()))
			throw new SemanticError("multiply method decleration" + method.getName(), method.getLine());
		else
		{
			method.setEnclosingScope(context);
			method.getType().accept(this, context);

			MethodTypeEntry methodEntry = null;
			Symbol methodSym = new Symbol(method.getName(), Kind.METHOD, methodEntry, true, Property.STATIC);
			SymbolTable scopeSelfContext = methodSym.getSymTableRef();
			scopeSelfContext.attachParentTable(context);

			for (Formal formal : method.getFormals())
			{
				Symbol formalSym = formal.accept(this, scopeSelfContext);
				scopeSelfContext.AddSymbol(formalSym);
			}

			methodEntry = new MethodTypeEntry(method.getType(), method.getFormals(), TypesTable.getCounter());
			methodSym.updateType(methodEntry);
			TypesTable.addMethodEntry(methodEntry);

			for (Statement stmt : method.getStatements())
			{
				Symbol stmtSymbol = stmt.accept(this, scopeSelfContext);
				if ((stmtSymbol != null) && ((stmtSymbol.getKind() == Kind.VAR_LOCAL) || (stmtSymbol.getKind() == Kind.STMTS_BLOCK)))
					scopeSelfContext.AddSymbol(stmtSymbol);
			}

			return methodSym;
		}
	}

	@Override
	public Symbol visit(Formal formal, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(formal.getName()))
			throw new SemanticError("duplicated formal decleration: " + formal.getName(), formal.getLine());
		else
		{
			formal.setEnclosingScope(context);
			formal.getType().accept(this, context);

			AbstractEntryTypeTable formalEntry = TypesTable.addSubDimsAndGetMyTypeEntry(formal.getType());
			Symbol formalSym = new Symbol(formal.getName(), Kind.FORMAL_PARAM, formalEntry, false);

			return formalSym;
		}
	}

	@Override
	public Symbol visit(PrimitiveType type, SymbolTable context) throws Exception{
		type.setEnclosingScope(context);
		TypesTable.addSubDimsAndGetMyTypeEntry(type);
		return null;
	}

	@Override
	public Symbol visit(UserType type, SymbolTable context) throws Exception{
		type.setEnclosingScope(context);
		TypesTable.addSubDimsAndGetMyTypeEntry(type);
		return null;
	}

	@Override
	public Symbol visit(Assignment assignment, SymbolTable context) throws Exception{
		assignment.setEnclosingScope(context);
		assignment.getAssignment().accept(this, context);
		assignment.getVariable().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(CallStatement callStatement, SymbolTable context) throws Exception{
		callStatement.setEnclosingScope(context);
		callStatement.getCall().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(Return returnStatement, SymbolTable context) throws Exception{
		returnStatement.setEnclosingScope(context);
		if (returnStatement.getValue() != null)
			returnStatement.getValue().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(If ifStatement, SymbolTable context) throws Exception{
		ifStatement.setEnclosingScope(context);
		ifStatement.getCondition().accept(this, context);
		Symbol operationSymbol = ifStatement.getOperation().accept(this, context);
		Statement elseStmt = ifStatement.getElseOperation();
		
		if ((operationSymbol != null) && (operationSymbol.getKind() == Kind.STMTS_BLOCK))
			context.AddSymbol(operationSymbol);
		if (elseStmt != null)
		{
			Symbol elseSymbol = elseStmt.accept(this, context);
			if ((elseSymbol != null) && (elseSymbol.getKind() == Kind.STMTS_BLOCK))
				context.AddSymbol(elseSymbol);
		}
		
		return null;
	}

	@Override
	public Symbol visit(While whileStatement, SymbolTable context) throws Exception{
		whileStatement.setEnclosingScope(context);
		whileStatement.getCondition().accept(this, context);
		Symbol operationSymbol = whileStatement.getOperation().accept(this, context);
		
		if ((operationSymbol != null) && (operationSymbol.getKind() == Kind.STMTS_BLOCK))
			context.AddSymbol(operationSymbol);
		return null;
	}

	@Override
	public Symbol visit(Break breakStatement, SymbolTable context) throws Exception{
		breakStatement.setEnclosingScope(context);
		return null;
	}

	@Override
	public Symbol visit(Continue continueStatement, SymbolTable context) throws Exception{
		continueStatement.setEnclosingScope(context);
		return null;
	}

	@Override
	public Symbol visit(StatementsBlock statementsBlock, SymbolTable context) throws Exception{
		statementsBlock.setEnclosingScope(context);
		Symbol stmtBlockSym = new Symbol(context.getGUID().toString(), Kind.STMTS_BLOCK, null, true, null, "statement block in " + context.getID());
		SymbolTable scopeSelfContext = stmtBlockSym.getSymTableRef();
		scopeSelfContext.attachParentTable(context);

		for (Statement stmt : statementsBlock.getStatements())
		{
			Symbol stmtSymbol = stmt.accept(this, scopeSelfContext);
			if ( (stmt != null) && ((stmt.getClass() == LocalVariable.class) || (stmt.getClass() == StatementsBlock.class)) )
				scopeSelfContext.AddSymbol(stmtSymbol);
		}

		return stmtBlockSym;
	}

	@Override
	public Symbol visit(LocalVariable localVariable, SymbolTable context) throws Exception{
		if (!context.isFirstDecleration(localVariable.getName()))
			throw new SemanticError("duplicated local variable decleration: " + localVariable.getName(), localVariable.getLine());
		else
		{
			localVariable.setEnclosingScope(context);
			localVariable.getType().accept(this, context);
			Expression e = localVariable.getInitValue();
			if (e != null)
				e.accept(this, context);

			AbstractEntryTypeTable varEntry = TypesTable.addSubDimsAndGetMyTypeEntry(localVariable.getType());
			Symbol localVarSym = new Symbol(localVariable.getName(), Kind.VAR_LOCAL, varEntry, false);

			return localVarSym;
		}
	}

	@Override
	public Symbol visit(VariableLocation location, SymbolTable context) throws Exception{
		location.setEnclosingScope(context);
		Expression e = location.getLocation();
		if (e != null)
			e.accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(ArrayLocation location, SymbolTable context) throws Exception{
		location.setEnclosingScope(context);
		location.getArray().accept(this, context);
		location.getIndex().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(StaticCall call, SymbolTable context) throws Exception{
		call.setEnclosingScope(context);

		for (Expression arg : call.getArguments())
			arg.accept(this, context);

		return null;
	}

	@Override
	public Symbol visit(VirtualCall call, SymbolTable context) throws Exception{
		call.setEnclosingScope(context);

		for (Expression arg : call.getArguments())
			arg.accept(this, context);
		Expression exprDotCall = call.getLocation();
		if (exprDotCall != null)
			exprDotCall.accept(this, context);
		//TODO:getlocation blablla should be before args?
		return null;
	}

	@Override
	public Symbol visit(This thisExpression, SymbolTable context) throws Exception{
		thisExpression.setEnclosingScope(context);
		return null;
	}

	@Override
	public Symbol visit(NewClass newClass, SymbolTable context) throws Exception{
		newClass.setEnclosingScope(context);
		return null;
	}

	@Override
	public Symbol visit(NewArray newArray, SymbolTable context) throws Exception{
		newArray.setEnclosingScope(context);
		newArray.getSize().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(Length length, SymbolTable context) throws Exception{
		length.setEnclosingScope(context);
		length.getArray().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(MathBinaryOp binaryOp, SymbolTable context) throws Exception{
		binaryOp.setEnclosingScope(context);
		binaryOp.getFirstOperand().accept(this, context);
		binaryOp.getSecondOperand().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(LogicalBinaryOp binaryOp, SymbolTable context) throws Exception{
		binaryOp.setEnclosingScope(context);
		binaryOp.getFirstOperand().accept(this, context);
		binaryOp.getSecondOperand().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(MathUnaryOp unaryOp, SymbolTable context) throws Exception{
		unaryOp.setEnclosingScope(context);
		unaryOp.getOperand().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(LogicalUnaryOp unaryOp, SymbolTable context) throws Exception{
		unaryOp.setEnclosingScope(context);
		unaryOp.getOperand().accept(this, context);
		return null;
	}

	@Override
	public Symbol visit(Literal literal, SymbolTable context) throws Exception{
		literal.setEnclosingScope(context);
		return null;
	}

	@Override
	public Symbol visit(ExpressionBlock expressionBlock, SymbolTable context) throws Exception{
		expressionBlock.setEnclosingScope(context);
		expressionBlock.getExpression().accept(this, context);
		return null;
	}

}
