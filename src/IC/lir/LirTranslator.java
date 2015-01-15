package IC.lir;

import IC.AST.*;
import IC.AST.Return;
import IC.AST.StaticCall;
import IC.AST.VirtualCall;
import IC.Symbols.Kind;
import IC.Symbols.Symbol;
import IC.Symbols.SymbolTable;
import IC.Types.ClassTypeEntry;
import IC.Types.PrimitiveTypeEnum;
import IC.Types.TypesTable;
import IC.lir.Instructions.*;
import com.sun.prism.RectShadowGraphics;
import microLIR.instructions.Reg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LirTranslator implements PropagatingVisitor<List<String>,List<String>> {

	public static final String VAL_OPTMZ = "";
	private Label currentWhileLabel;
	private Label currentEndWhileLabel;
    List<String> mainBlock;

	@Override
	public List<String> visit(Program program, List<String> target) throws Exception {
		DispacthTableBuilder.init(program);
		DispacthTableBuilder.buildClassLayouts();

		List<String> instructionList = new LinkedList<String>();
		for (ICClass icClass : program.getClasses()) {
			instructionList.addAll(icClass.accept(this, null));
		}
        instructionList.addAll(mainBlock);
		instructionList.add(0, new BlankLine().toString());
		instructionList.addAll(0, CompileTimeData.getDispatchTables());
		instructionList.addAll(0, CompileTimeData.getStringLiterals());
		return instructionList;
	}

	@Override
	//TODO: check if _ic_main need to be at the end of the lir program
	public List<String> visit(ICClass icClass, List<String> target) throws Exception {
		if (icClass.getName().equals("Library")) {
			return new LinkedList<String>();
		}
		String className = icClass.getName();
		ClassLayout classLayout = CompileTimeData.getClassLayout(className);
		DispatchTable classDT = new DispatchTable(className, classLayout);
		CompileTimeData.addDispatchTable(classDT);

		List<String> classInstructions = new LinkedList<String>();
		String currentClass = icClass.getName();
		for (Method method : icClass.getMethods()) {
			List<String> methodInstructions = new LinkedList<String>();
			Label methodLabel;
			if ( (method instanceof StaticMethod) && (method.getName().equals("main")) )
				methodLabel = new Label("_ic_" + method.getName());
			else
				methodLabel = new Label("_" + currentClass + "_" + method.getName());

			if (!classInstructions.isEmpty())
				methodInstructions.add(new BlankLine().toString());
			methodInstructions.add(methodLabel.toString());
			methodInstructions.addAll(method.accept(this, null));
            if ((method instanceof StaticMethod) && (method.getName().equals("main"))) {
                mainBlock = methodInstructions;
            }
            else {
                classInstructions.addAll(methodInstructions);
            }
		}
		classInstructions.add(new BlankLine().toString());
		return classInstructions;
	}

	@Override
	public List<String> visit(Field field, List<String> target) throws Exception { throw new Exception("shouldn't be invoked..."); }

	@Override
	public List<String> visit(VirtualMethod method, List<String> target) throws Exception {
		List<String> methodInstructions = new LinkedList<String>();
		for (Statement statement : method.getStatements()) {
			methodInstructions.addAll(statement.accept(this, null));
		}
		return methodInstructions;
	}

	@Override
	public List<String> visit(StaticMethod method, List<String> target) throws Exception {
		List<String> methodInstructions = new LinkedList<String>();
		for (Statement statement : method.getStatements()) {
			methodInstructions.addAll(statement.accept(this, null));
		}
		return methodInstructions;
	}

	@Override
	public List<String> visit(LibraryMethod method, List<String> target) throws Exception { return new LinkedList<String>(); }

	@Override
	public List<String> visit(Formal formal, List<String> target) throws Exception { throw new Exception("shouldn't be invoked..."); }

	@Override
	public List<String> visit(PrimitiveType type, List<String> target) throws Exception { throw new Exception("shouldn't be invoked..."); }

	@Override
	public List<String> visit(UserType type, List<String> target) throws Exception { throw new Exception("shouldn't be invoked..."); }

	@Override
	public List<String> visit(Assignment assignment, List<String> target) throws Exception {
		List<String> assignmentLirLineList = new LinkedList<String>();

		Expression assignExpr = assignment.getAssignment();
		List<String> assignRegs = new ArrayList<String>();
		assignRegs.add(LirTranslator.VAL_OPTMZ);
		List<String> assignTR = assignExpr.accept(this, assignRegs);

		Location location = assignment.getVariable();
		List<String> locationRegs = new ArrayList<String>();
		List<String> locationTR = location.accept(this, locationRegs);

		assignmentLirLineList.addAll(assignTR);
		assignmentLirLineList.addAll(locationTR);

		BinaryInstruction assignInst;
		String assignOp = assignRegs.get(0);
		String locationOp;
		if (location instanceof ArrayLocation)
		{
			// locationRegs = {base, index}, index can be immediate/reg
					// assignRegs = immediate/reg/memory
			String locationResult = locationRegs.get(0);
			String indexResult = locationRegs.get(1);
			String auxReg;
			String auxReg2;

			if (CompileTimeData.isRegName(locationResult)) {
				auxReg = locationResult;
			}
			else {
				auxReg = RegisterFactory.allocateRegister();
				BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, locationResult, auxReg);
				assignmentLirLineList.add(mem2reg.toString());
			}

			if (CompileTimeData.isMemory(indexResult)) {
				auxReg2 = RegisterFactory.allocateRegister();
				BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, indexResult, auxReg2);
				assignmentLirLineList.add(mem2reg.toString());
			}
			else {
				auxReg2 = indexResult;
			}

			locationOp = auxReg + "[" + auxReg2 + "]";

			if (CompileTimeData.isImmediate(assignOp))
			{
				assignInst= new BinaryInstruction(LirBinaryOps.MOVEARRAY, assignOp, locationOp);
				assignmentLirLineList.add(assignInst.toString());
			}	
			else if (CompileTimeData.isRegName(assignOp))
			{
				assignInst= new BinaryInstruction(LirBinaryOps.MOVEARRAY, assignOp, locationOp);
				assignmentLirLineList.add(assignInst.toString());
				RegisterFactory.freeRegister(assignOp);
			}
			else // memory (assignOp is strX or local var)
			{
				String tempReg = RegisterFactory.allocateRegister();
				BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, assignOp, tempReg);
				assignmentLirLineList.add(getMem.toString());
				assignInst= new BinaryInstruction(LirBinaryOps.MOVEARRAY, tempReg, locationOp);
				assignmentLirLineList.add(assignInst.toString());
				RegisterFactory.freeRegister(tempReg);
			}

			if (CompileTimeData.isRegName(auxReg))
				RegisterFactory.freeRegister(auxReg);
			if (CompileTimeData.isRegName(auxReg2))
				RegisterFactory.freeRegister(auxReg2);
		}

		else if (location instanceof VariableLocation) {
			//locationRegs = {memory}/{reg,immediate}
			//assignRegs = (immediate/reg/memory)
			if (!((VariableLocation) location).isExternal() && (locationRegs.size() == 1)) {//location = var and it's not this.var
				locationOp = locationRegs.get(0);
				if (CompileTimeData.isImmediate(assignOp))
				{
					assignInst= new BinaryInstruction(LirBinaryOps.MOVE, assignOp, locationOp);
					assignmentLirLineList.add(assignInst.toString());
				}
				else if (CompileTimeData.isRegName(assignOp))
				{
					assignInst= new BinaryInstruction(LirBinaryOps.MOVE, assignOp, locationOp);
					assignmentLirLineList.add(assignInst.toString());
					RegisterFactory.freeRegister(assignOp);
				}
				else // memory (assignOp is strX or local var)
				{
					String tempReg = RegisterFactory.allocateRegister();
					BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, assignOp, tempReg);
					assignmentLirLineList.add(getMem.toString());
					assignInst= new BinaryInstruction(LirBinaryOps.MOVE, tempReg, locationOp);
					assignmentLirLineList.add(assignInst.toString());
					RegisterFactory.freeRegister(tempReg);
				}
			}
			else { //location = exp.var
				locationOp = locationRegs.get(0) + "." + locationRegs.get(1);
				if (CompileTimeData.isImmediate(assignOp))
				{
					assignInst= new BinaryInstruction(LirBinaryOps.MOVEFIELD, assignOp, locationOp);
					assignmentLirLineList.add(assignInst.toString());
				}
				else if (CompileTimeData.isRegName(assignOp))
				{
					assignInst= new BinaryInstruction(LirBinaryOps.MOVEFIELD, assignOp, locationOp);
					assignmentLirLineList.add(assignInst.toString());
					RegisterFactory.freeRegister(assignOp);
				}
				else // memory (assignOp is strX or local var)
				{
					String tempReg = RegisterFactory.allocateRegister();
					BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, assignOp, tempReg);
					assignmentLirLineList.add(getMem.toString());
					assignInst= new BinaryInstruction(LirBinaryOps.MOVEFIELD, tempReg, locationOp);
					assignmentLirLineList.add(assignInst.toString());
					RegisterFactory.freeRegister(tempReg);
				}

				if (CompileTimeData.isRegName(locationRegs.get(0)))
					RegisterFactory.freeRegister(locationRegs.get(0));
			}
		}

		return assignmentLirLineList;
	}

	@Override
	public List<String> visit(CallStatement callStatement, List<String> targetRegisters) throws Exception {
		List<String> callStatementBlock = new LinkedList<String>();
		Call call = callStatement.getCall();
		List<String> callStatementRegisters = new LinkedList<String>();
		List<String> callStatementTR = call.accept(this, callStatementRegisters);
		callStatementBlock.addAll(callStatementTR);
		for (String target : callStatementRegisters) {
			if (CompileTimeData.isRegName(target)) {
				RegisterFactory.freeRegister(target);
			}
		}
		return callStatementBlock;
	}

	@Override
	public List<String> visit(Return returnStatement, List<String> targetRegisters) throws Exception {
		List<String> returnStatementBlock = new LinkedList<String>();
		if (returnStatement.hasValue()) {
			Expression returnValue = returnStatement.getValue();
			List<String> returnStatementRegisters = new LinkedList<String>();
			returnStatementRegisters.add(VAL_OPTMZ);
			List<String> returnValueTR = returnValue.accept(this, returnStatementRegisters);
			returnStatementBlock.addAll(returnValueTR);
			UnaryInstruction returnInstruction = new UnaryInstruction(LirUnaryOps.RETURN, returnStatementRegisters.get(0));
			returnStatementBlock.add(returnInstruction.toString());
		}
		else {
			UnaryInstruction returnInstruction = new UnaryInstruction(LirUnaryOps.RETURN, "9999");
			returnStatementBlock.add(returnInstruction.toString());
		}

		return returnStatementBlock;
	}

	@Override
	public List<String> visit(If ifStatement, List<String> targetRegisters) throws Exception {
		List<String> ifStatementBlock = new LinkedList<String>();
		Expression condition = ifStatement.getCondition();
		List<String> conditionRegisters = new LinkedList<String>(); //add hack?
		List<String> conditionTR = condition.accept(this, conditionRegisters);
		ifStatementBlock.addAll(conditionTR);
		String conditionRegister = conditionRegisters.get(0);
		String auxReg;
		if (CompileTimeData.isRegName(conditionRegister)) {
			auxReg = conditionRegister;
		}
		else {
			auxReg = RegisterFactory.allocateRegister();
		}
		BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, "0", auxReg);
		ifStatementBlock.add(checkCondition.toString());
		RegisterFactory.freeRegister(auxReg);

		Label endLabel = new Label("_end_if_label_" + ifStatement.getLine());
		Statement operation = ifStatement.getOperation();
		List<String> operationTR = operation.accept(this, null);

		if (!ifStatement.hasElse()){
			UnaryInstruction falseCondition = new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel);
			ifStatementBlock.add(falseCondition.toString());
			ifStatementBlock.addAll(operationTR);
			ifStatementBlock.add(endLabel.toString());
		}

		else {
			Label falseLabel = new Label("_false_label_" + ifStatement.getLine());
			UnaryInstruction falseCondition = new UnaryInstruction(LirUnaryOps.JUMPTRUE, falseLabel);
			ifStatementBlock.add(falseCondition.toString());
			ifStatementBlock.addAll(operationTR);
			UnaryInstruction endIf = new UnaryInstruction(LirUnaryOps.JUMP, endLabel);
			ifStatementBlock.add(endIf.toString());
			ifStatementBlock.add(falseLabel.toString());
			Statement elseOperation = ifStatement.getElseOperation();
			List<String> elseOperationTR = elseOperation.accept(this, null);
			ifStatementBlock.addAll(elseOperationTR);
			ifStatementBlock.add(endLabel.toString());
		}

		return ifStatementBlock;
	}

	@Override
	public List<String> visit(While whileStatement, List<String> targetRegisters) throws Exception {
		List<String> whileStatementBlock = new LinkedList<String>();
		currentWhileLabel = new Label("_while_label_" + whileStatement.getLine());
		currentEndWhileLabel = new Label("_end_while_label_" + whileStatement.getLine());

		whileStatementBlock.add(currentWhileLabel.toString());

		Expression condition = whileStatement.getCondition();
		List<String> conditionRegisters = new LinkedList<String>(); //add hack?
		List<String> conditionTR = condition.accept(this, conditionRegisters);
		whileStatementBlock.addAll(conditionTR);

		String conditionRegister = conditionRegisters.get(0);
		String auxReg;
		if (CompileTimeData.isRegName(conditionRegister)) {
			auxReg = conditionRegister;
		}
		else {
			auxReg = RegisterFactory.allocateRegister();
		}
		BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, "0", auxReg);
		whileStatementBlock.add(checkCondition.toString());
		UnaryInstruction falseCondition = new UnaryInstruction(LirUnaryOps.JUMPTRUE, currentEndWhileLabel);
		whileStatementBlock.add(falseCondition.toString());
		Statement operation = whileStatement.getOperation();
		List<String> operationTR = operation.accept(this, null);
		whileStatementBlock.addAll(operationTR);
		UnaryInstruction startOver = new UnaryInstruction(LirUnaryOps.JUMP, currentWhileLabel);
		whileStatementBlock.add(startOver.toString());
		whileStatementBlock.add(currentEndWhileLabel.toString());
		RegisterFactory.freeRegister(auxReg);
		return whileStatementBlock;
	}

	@Override
	public List<String> visit(Break breakStatement, List<String> targetRegisters) throws Exception {
		List<String> breakStatementBlock = new LinkedList<String>();
		breakStatementBlock.add(currentEndWhileLabel.toString());
		return breakStatementBlock;
	}

	@Override
	public List<String> visit(Continue continueStatement, List<String> targetRegisters) throws Exception {
		List<String> continueStatementBlock = new LinkedList<String>();
		continueStatementBlock.add(currentWhileLabel.toString());
		return continueStatementBlock;
	}

	@Override
	public List<String> visit(StatementsBlock statementsBlock, List<String> targetRegisters) throws Exception {
		List<String> statementBlockBlock = new LinkedList<String>();
		for (Statement statement : statementsBlock.getStatements()) {
			List<String> statementTR = statement.accept(this, null);
			statementBlockBlock.addAll(statementTR);
		}
		return statementBlockBlock;
	}

	@Override
	public List<String> visit(LocalVariable localVariable, List<String> targetRegisters) throws Exception {
		List<String> localVariableBlock = new LinkedList<String>();
		if (localVariable.hasInitValue()) {
			Expression initValue = localVariable.getInitValue();
			List<String> initValueRegisters = new LinkedList<String>();
			initValueRegisters.add(VAL_OPTMZ);
			List<String> initValueTR = initValue.accept(this, initValueRegisters);
			localVariableBlock.addAll(initValueTR);
			String value = initValueRegisters.get(0);
			BinaryInstruction initializeVar;
			if (!CompileTimeData.isMemory(value)) {
				initializeVar = new BinaryInstruction(LirBinaryOps.MOVE, value, localVariable.getName());
				if (CompileTimeData.isRegName(value)) {
					RegisterFactory.freeRegister(value);
				}
			} else {
				String tempRegister = RegisterFactory.allocateRegister();
				BinaryInstruction memToTemp = new BinaryInstruction(LirBinaryOps.MOVE, value, tempRegister);
				localVariableBlock.add(memToTemp.toString());
				initializeVar = new BinaryInstruction(LirBinaryOps.MOVE, tempRegister, localVariable.getName());
				RegisterFactory.freeRegister(tempRegister);
			}
			localVariableBlock.add(initializeVar.toString());
		}
		return localVariableBlock;
	}

	@Override
	public List<String> visit(VariableLocation location, List<String> target) throws Exception {
		location.setAndGetRegWeight();
		List<String> variableLocationLirLineList = new LinkedList<String>();

		SymbolTable currentScope = location.getEnclosingScope();
		Boolean isNotField = true;
		Boolean foundDeclaration = false;
		String id = location.getName();
		String fieldClassOwner = "";
		while (!foundDeclaration && (currentScope != null))
		{
			String classID = currentScope.getID();
			if (currentScope.getSymbolByID(id) != null)
			{
				foundDeclaration = true;
				if (CompileTimeData.isAlreadyBuilt(classID))
				{
					isNotField = false;
					fieldClassOwner = classID;
				}
			}
			currentScope = currentScope.getParentTable();
		}

		if (!location.isExternal() && isNotField) {
			if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
				target.remove(0);
			target.add(location.getName());
		}
		else {
			String exprOp;
			String className;

			if (location.isExternal())
			{
				List<String> baseRegs = new ArrayList<String>();
				baseRegs.add(VAL_OPTMZ);
				Expression baseExpr = location.getLocation();
				List<String> baseLirInstructions = baseExpr.accept(this, baseRegs);

				//baseRegs contains memory/reg, both cases had to return target in this form {regX, offset} / {RegX}
				variableLocationLirLineList.addAll(baseLirInstructions);
				exprOp = baseRegs.get(0);
//                BinaryInstruction checkNullRef = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkNullRef(" + exprOp + ")", "Rdummy");
//                variableLocationLirLineList.add(checkNullRef.toString()); TODO: uncomment
				className = ((ClassTypeEntry)baseExpr.getAssignedType()).getName();

			}
			else // happened when "variableLocation == ID" and ID is a field, aka this.ID
			{
				exprOp = "this";
				className = fieldClassOwner;
			}


			if (!CompileTimeData.isRegName(exprOp)) // memory
			{
				String exprReg = RegisterFactory.allocateRegister();
				BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, exprOp, exprReg);
				variableLocationLirLineList.add(getMem.toString());
				exprOp = exprReg;
			}

			// need to calculate the offset
			ClassLayout classLayout = CompileTimeData.getClassLayout(className);
			int offset = classLayout.getFieldOffset(location.getName());
			String offsetStr = String.valueOf(offset);
			// VAL_OPTMZ
			if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
			{
				target.remove(0);
				BinaryInstruction optmz = new BinaryInstruction(LirBinaryOps.MOVEFIELD, exprOp + "." + offsetStr, exprOp);
				variableLocationLirLineList.add(optmz.toString());
				target.add(exprOp);
			}
			else
			{
				target.add(exprOp);
				target.add(offsetStr);
			}
		}
		return variableLocationLirLineList;
	}

	@Override
	public List<String> visit(ArrayLocation location, List<String> target) throws Exception {
		// expr[reg], or expr[immediate]
		location.setAndGetRegWeight();
		List<String> arrayLocationLirLineList = new LinkedList<String>();

		List<String> arrayRegs = new ArrayList<String>();
		arrayRegs.add(VAL_OPTMZ);
		Expression arrayExpr = location.getArray();
		List<String> arrayLirInstructions = arrayExpr.accept(this, arrayRegs);
		arrayLocationLirLineList.addAll(arrayLirInstructions);
		String arrayOp = arrayRegs.get(0);
//		BinaryInstruction checkNullRef = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkNullRef(" + arrayOp + ")", "Rdummy");
//		arrayLocationLirLineList.add(checkNullRef.toString()); TODO: uncomment

		List<String> indexRegs = new ArrayList<String>();
		indexRegs.add(VAL_OPTMZ);
		Expression indexExpr = location.getIndex();
		List<String> indexLirInstructions = indexExpr.accept(this, indexRegs);
		arrayLocationLirLineList.addAll(indexLirInstructions);
		String indexOp = indexRegs.get(0);
//		BinaryInstruction checkArrayAccess = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkArrayAccess(" + arrayOp + "," + indexOp + ")", "Rdummy");
//		arrayLocationLirLineList.add(checkArrayAccess.toString()); TODO: uncomment

		if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ)) {
			String targetReg, auxReg, auxReg2;
			target.remove(0);

			if (CompileTimeData.isRegName(arrayOp)) {
				targetReg = auxReg = arrayOp;
			} else {
				auxReg = RegisterFactory.allocateRegister();
				BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, arrayOp, auxReg);
				arrayLocationLirLineList.add(mem2reg.toString());
				if (CompileTimeData.isRegName(indexOp)) {
					targetReg = indexOp;
				} else {
					targetReg = auxReg;
				}
			}

			if (CompileTimeData.isMemory(indexOp)) {
				auxReg2 = RegisterFactory.allocateRegister();
				BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, indexOp, auxReg2);
				arrayLocationLirLineList.add(mem2reg.toString());
			}
			else {
				auxReg2 = indexOp;
			}

			BinaryInstruction optmz = new BinaryInstruction(LirBinaryOps.MOVEARRAY, auxReg + "[" + auxReg2 + "]", targetReg);
			arrayLocationLirLineList.add(optmz.toString());
			target.add(targetReg);
			if (CompileTimeData.isRegName(auxReg) && !auxReg.equals(targetReg)) {
				RegisterFactory.freeRegister(auxReg);
			}
			if (CompileTimeData.isRegName(auxReg2) && !auxReg2.equals(targetReg)) {
				RegisterFactory.freeRegister(auxReg2);
			}
		}
		else {
			target.add(arrayOp);
			target.add(indexOp);
		}
		return arrayLocationLirLineList;
	}

	@Override
	public List<String> visit(StaticCall call, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> callBlock = new LinkedList<String>();
		String lirCall;
		List<String> argumentRegisters;
		List<String> argumentTR;
		List<String> registersToFree = new LinkedList<String>();
		BinaryInstruction callInstruction;
		String targetRegister;

		if (call.getClassName().equals("Library")) {
			lirCall = "__" + call.getName() + "(";
			for (Expression argument : call.getArguments()) {
				argumentRegisters = new LinkedList<String>();
				argumentRegisters.add(VAL_OPTMZ);
				argumentTR = argument.accept(this, argumentRegisters);
				callBlock.addAll(argumentTR);
				String argumentValue = argumentRegisters.get(0);
				if (!(call.getArguments().indexOf(argument) == 0)) {
					lirCall += ",";
				}
				lirCall += argumentValue;
				if (CompileTimeData.isRegName(argumentValue)) {
					registersToFree.add(argumentValue);
				}
			}
			lirCall += ")";
			for (String register : registersToFree) {
				RegisterFactory.freeRegister(register);
			}
			targetRegister = RegisterFactory.allocateRegister();
			callInstruction = new BinaryInstruction(LirBinaryOps.LIBRARY, lirCall, targetRegister);
		}

		else {
			String methodName = call.getName();
			String callingClass = call.getClassName();
			String declatingClass = CompileTimeData.getClassLayout(callingClass).getDeclaringMap().get(methodName);
			lirCall = declatingClass + "_" + methodName + "(";
			SymbolTable methodSymbolTable = SymbolTable.getTopTable().getSymbolByID(declatingClass).getSymTableRef().getSymbolByID(methodName).getSymTableRef();
			List<String> formalNames = new LinkedList<String>();
			for (Symbol symbol : methodSymbolTable.getSymbols()) {
				if (symbol.getKind() == Kind.FORMAL_PARAM)
					formalNames.add(symbol.getId());
			}
			Iterator<String> formalIterator = formalNames.iterator();
			Iterator<Expression> argumentsIterator = call.getArguments().iterator();
			while (formalIterator.hasNext()) {
				String formalName = formalIterator.next();
				Expression argument = argumentsIterator.next();
				argumentRegisters = new LinkedList<String>();
				argumentRegisters.add(VAL_OPTMZ);
				argumentTR = argument.accept(this, argumentRegisters);
				callBlock.addAll(argumentTR);
				String argumentValue = argumentRegisters.get(0);
				if (!(call.getArguments().indexOf(argument) == 0)) {
					lirCall += ",";
				}
				lirCall += formalName + "=" + argumentValue;
				if (CompileTimeData.isRegName(argumentValue)) {
					registersToFree.add(argumentValue);
				}
			}
			lirCall += ")";
			for (String register : registersToFree) {
				RegisterFactory.freeRegister(register);
			}
			targetRegister = RegisterFactory.allocateRegister();
			callInstruction = new BinaryInstruction(LirBinaryOps.STATICCALL, lirCall, targetRegister);
		}

		callBlock.add(callInstruction.toString());
		targetRegisters.add(targetRegister);
		return callBlock;
	}

	@Override
	public List<String> visit(VirtualCall call, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> callBlock = new LinkedList<String>();

		String instanceRegister;
		BinaryInstruction initializeInstanceRegister;
		int methodOffset;
		String methodName = call.getName();
		String className;

		if (call.isExternal()) {
			Expression location = call.getLocation();
			List<String> locationRegisters = new LinkedList<String>();
			locationRegisters.add(VAL_OPTMZ);
			List<String> locationTR = location.accept(this, locationRegisters);
			callBlock.addAll(locationTR);
			String targetlocationTR = locationRegisters.get(0);
//            BinaryInstruction checkNullRef = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkNullRef(" + targetlocationTR + ")", "Rdummy");
//            callBlock.add(checkNullRef.toString()); TODO: uncomment
			if (CompileTimeData.isRegName(targetlocationTR)) {
				instanceRegister = targetlocationTR;
			}
			else {
				instanceRegister = RegisterFactory.allocateRegister();
				initializeInstanceRegister = new BinaryInstruction(LirBinaryOps.MOVE, targetlocationTR, instanceRegister);
				callBlock.add(initializeInstanceRegister.toString());
			}

			// offset

			className = ((ClassTypeEntry)location.getAssignedType()).getName();
			methodOffset = CompileTimeData.getClassLayout(className).getMethodOffset(methodName);
		}
		else {
			instanceRegister = RegisterFactory.allocateRegister();
			initializeInstanceRegister = new BinaryInstruction(LirBinaryOps.MOVE, "this", instanceRegister);
			callBlock.add(initializeInstanceRegister.toString());

			// find the lowest enclosing scope that is also a classDecl
			SymbolTable currentScope = call.getEnclosingScope();
			className = "";
			while (currentScope != null)
			{
				String scopeID = currentScope.getID();
				if (CompileTimeData.isAlreadyBuilt(scopeID)) //translator runs only after completion of classesLayout build
					className = scopeID;
				currentScope = currentScope.getParentTable();
			}
			methodOffset = CompileTimeData.getClassLayout(className).getMethodOffset(methodName);
		}

		String lirCall = instanceRegister + "." + methodOffset + "(";

		List<String> argumentRegisters;
		List<String> argumentTR;
		List<String> registersToFree = new LinkedList<String>();
		BinaryInstruction callInstruction;

		String declatingClass = CompileTimeData.getClassLayout(className).getDeclaringMap().get(methodName);
		SymbolTable methodSymbolTable = SymbolTable.getTopTable().getSymbolByID(declatingClass).getSymTableRef().getSymbolByID(methodName).getSymTableRef();
		List<String> formalNames = new LinkedList<String>();
		for (Symbol symbol : methodSymbolTable.getSymbols()) {
			if (symbol.getKind() == Kind.FORMAL_PARAM)
				formalNames.add(symbol.getId());
		}
		Iterator<String> formalIterator = formalNames.iterator();
		Iterator<Expression> argumentsIterator = call.getArguments().iterator();
		while (formalIterator.hasNext()) {
			String formalName = formalIterator.next();
			Expression argument = argumentsIterator.next();
			argumentRegisters = new LinkedList<String>();
			argumentRegisters.add(VAL_OPTMZ);
			argumentTR = argument.accept(this, argumentRegisters);
			callBlock.addAll(argumentTR);
			String argumentValue = argumentRegisters.get(0);
			if (!(call.getArguments().indexOf(argument) == 0)) {
				lirCall += ",";
			}
			lirCall += formalName + "=" + argumentValue;
			if (CompileTimeData.isRegName(argumentValue)) {
				registersToFree.add(argumentValue);
			}
		}
		lirCall += ")";
		for (String register : registersToFree) {
			RegisterFactory.freeRegister(register);
		}
		callInstruction = new BinaryInstruction(LirBinaryOps.VIRTUALCALL, lirCall, instanceRegister);

		callBlock.add(callInstruction.toString());
		targetRegisters.add(instanceRegister);
		return callBlock;
	}

	@Override
	public List<String> visit(This thisExpression, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		targetRegisters.add("this"); //that's it???
		return new LinkedList<String>();
	}

	@Override
	public List<String> visit(NewClass newClass, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> newClassBlock = new LinkedList<String>();
		int objectSize = CompileTimeData.getClassLayout(newClass.getName()).getFieldToOffsetSize() + 1;
		objectSize *= 4;
		String targetRegister = RegisterFactory.allocateRegister();
		BinaryInstruction allocateObject = new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateObject(" + objectSize + ")", targetRegister);
		newClassBlock.add(allocateObject.toString());
		BinaryInstruction addDVPTR = new BinaryInstruction(LirBinaryOps.MOVEFIELD, "_DV_" + newClass.getName(), targetRegister + ".0");
		newClassBlock.add(addDVPTR.toString());
		targetRegisters.add(targetRegister);
		return newClassBlock;
	}

	@Override
	public List<String> visit(NewArray newArray, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> newArrayBlock = new LinkedList<String>();
		Expression sizeExpression = newArray.getSize();
		List<String> sizeExpressionRegisters = new LinkedList<String>();
		sizeExpressionRegisters.add(VAL_OPTMZ);
		List<String> sizeExpressionTR = sizeExpression.accept(this, sizeExpressionRegisters);
		newArrayBlock.addAll(sizeExpressionTR);
		String sizeResult = sizeExpressionRegisters.get(0);

//        BinaryInstruction checkSize = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkSize(" + sizeResult + ")", "Rdummy");
//        newArrayBlock.add(checkSize.toString()); TODO: uncomment

		String sizeTimes4Reg;
		BinaryInstruction allocateArray;
		if (CompileTimeData.isImmediate(sizeResult)) {
			sizeTimes4Reg = String.valueOf(Integer.valueOf(sizeResult) * 4);
			String target = RegisterFactory.allocateRegister();
			allocateArray =  new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeTimes4Reg + ")", target);
			newArrayBlock.add(allocateArray.toString());
			targetRegisters.add(target);
		}
		else if (CompileTimeData.isMemory(sizeResult)) {
			sizeTimes4Reg = RegisterFactory.allocateRegister();
			BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, sizeResult, sizeTimes4Reg);
			newArrayBlock.add(mem2reg.toString());
			BinaryInstruction mulBy4 = new BinaryInstruction(LirBinaryOps.MUL, "4", sizeTimes4Reg);
			newArrayBlock.add(mulBy4.toString());
			allocateArray =  new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeTimes4Reg + ")", sizeTimes4Reg);
			newArrayBlock.add(allocateArray.toString());
			targetRegisters.add(sizeTimes4Reg);
		}
		else {
			BinaryInstruction mulBy4 = new BinaryInstruction(LirBinaryOps.MUL, "4", sizeResult);
			newArrayBlock.add(mulBy4.toString());
			allocateArray =  new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeResult + ")", sizeResult);
			newArrayBlock.add(allocateArray.toString());
			targetRegisters.add(sizeResult);
		}
		return newArrayBlock;
	}

	@Override
	public List<String> visit(Length length, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> lengthBlock = new LinkedList<String>();
		Expression arrayExpression = length.getArray();
		List<String> arrayExpressionRegisters = new LinkedList<String>();
		arrayExpressionRegisters.add(VAL_OPTMZ);
		List<String> arrayExpressionTR = arrayExpression.accept(this, arrayExpressionRegisters);
		lengthBlock.addAll(arrayExpressionTR);
		String arrayExpressionResult = arrayExpressionRegisters.get(0);
//        BinaryInstruction checkNullRef = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkNullRef(" + arrayExpressionResult + ")", "Rdummy");
//        lengthBlock.add(checkNullRef.toString()); TODO: uncomment
		String targetRegister;
		if (CompileTimeData.isRegName(arrayExpressionResult)) {
			targetRegister = arrayExpressionResult;
		}
		else {
			targetRegister = RegisterFactory.allocateRegister();
		}
		BinaryInstruction lengthInstruction = new BinaryInstruction(LirBinaryOps.ARRAYLENGTH, arrayExpressionResult, targetRegister);
		lengthBlock.add(lengthInstruction.toString());
		targetRegisters.add(targetRegister);
		return lengthBlock;
	}

	@Override
	public List<String> visit(MathBinaryOp binaryOp, List<String> target) throws Exception {

		if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
			target.remove(0);
		Expression leftOperator = binaryOp.getFirstOperand();
		Expression rightOperator = binaryOp.getSecondOperand();
		/*int leftWeight = leftOperator.setAndGetRegWeight();
    	int rightWeight = rightOperator.setAndGetRegWeight();*/

		List<String> binaryOpLirLineList = new LinkedList<String>();
		List<String> leftOpInstructions;
		List<String> rightOpInstructions;

		List<String> leftOpRegs = new ArrayList<String>();
		List<String> rightOpRegs = new ArrayList<String>();

		leftOpRegs.add(VAL_OPTMZ);
		rightOpRegs.add(VAL_OPTMZ);

		// no side effects!
		if (binaryOp.setAndGetRegWeight() != -1)
		{
			if (rightOperator.setAndGetRegWeight() > leftOperator.setAndGetRegWeight())
			{
				rightOpInstructions = rightOperator.accept(this, rightOpRegs);
				// r_weight > l_weight => r_weight >=1 => register exist!
				binaryOpLirLineList.addAll(rightOpInstructions);
				String reg = rightOpRegs.get(0);
                
				leftOpInstructions = leftOperator.accept(this, leftOpRegs);
				binaryOpLirLineList.addAll(leftOpInstructions);
				// leftOpRegs contains memory/immediate
				String leftOp = leftOpRegs.get(0);

				BinaryInstruction inst;
				switch (binaryOp.getOperator()) {
				case DIVIDE:
//                    BinaryInstruction checkZero = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkZero(" + reg + ")", "Rdummy");
//                    binaryOpLirLineList.add(checkZero.toString()); TODO: uncomment
					String temp = RegisterFactory.allocateRegister();
					BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp);
					binaryOpLirLineList.add(getMem.toString());
					inst = new BinaryInstruction(LirBinaryOps.DIV, reg, temp);
					binaryOpLirLineList.add(inst.toString());
					if (CompileTimeData.isRegName(reg))
					{
						BinaryInstruction optmz = new BinaryInstruction(LirBinaryOps.MOVE, temp, reg);
						binaryOpLirLineList.add(optmz.toString());
						RegisterFactory.freeRegister(temp);
						target.add(reg);
					}
					else
					{
						target.add(temp);
					}
					break;
				case MINUS:
					String temp2 = RegisterFactory.allocateRegister();
					BinaryInstruction getMem2 = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp2);
					binaryOpLirLineList.add(getMem2.toString());
					inst = new BinaryInstruction(LirBinaryOps.SUB, reg, temp2);
					binaryOpLirLineList.add(inst.toString());
					if (CompileTimeData.isRegName(reg))
					{
						BinaryInstruction optmz2 = new BinaryInstruction(LirBinaryOps.MOVE, temp2, reg);
						binaryOpLirLineList.add(optmz2.toString());
						RegisterFactory.freeRegister(temp2);
						target.add(reg);
					}
					else
					{
						target.add(temp2);
					}
					break;
				case MOD:
					String temp3 = RegisterFactory.allocateRegister();
					BinaryInstruction getMem3 = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp3);
					binaryOpLirLineList.add(getMem3.toString());
					inst = new BinaryInstruction(LirBinaryOps.MOD, reg, temp3);
					binaryOpLirLineList.add(inst.toString());
					if (CompileTimeData.isRegName(reg))
					{
						BinaryInstruction optmz3 = new BinaryInstruction(LirBinaryOps.MOVE, temp3, reg);
						binaryOpLirLineList.add(optmz3.toString());
						RegisterFactory.freeRegister(temp3);
						target.add(reg);
					}
					else
					{
						target.add(temp3);
					}
					break;
				case MULTIPLY:
					if (!CompileTimeData.isRegName(reg))
					{
						String temp4 = RegisterFactory.allocateRegister();
						BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, reg, temp4);
						binaryOpLirLineList.add(mem2reg.toString());
						reg = temp4;
					}
					inst = new BinaryInstruction(LirBinaryOps.MUL, leftOp, reg);
					binaryOpLirLineList.add(inst.toString());
					target.add(reg);
					break;
				case PLUS:
					if (!CompileTimeData.isRegName(reg))
					{
						String temp5 = RegisterFactory.allocateRegister();
						BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, reg, temp5);
						binaryOpLirLineList.add(mem2reg.toString());
						reg = temp5;
					}
					inst = new BinaryInstruction(LirBinaryOps.ADD, leftOp, reg);
					binaryOpLirLineList.add(inst.toString());
					target.add(reg);
					break;
				default:
					break;
				}
				if (CompileTimeData.isRegName(leftOp))
					RegisterFactory.freeRegister(leftOp);
				return binaryOpLirLineList;
			}
		}
		// have side effects OR left heavier or same than "random"  => left is first

		leftOpInstructions = leftOperator.accept(this, leftOpRegs);
		binaryOpLirLineList.addAll(leftOpInstructions);
		String leftOp = leftOpRegs.get(0);

		rightOpInstructions = rightOperator.accept(this, rightOpRegs);
		binaryOpLirLineList.addAll(rightOpInstructions);
		String rightOp = rightOpRegs.get(0);

		BinaryInstruction inst;
		switch (binaryOp.getOperator()) {
		case DIVIDE:
            BinaryInstruction checkZero = new BinaryInstruction(LirBinaryOps.LIBRARY, "__checkZero(" + rightOp + ")", "Rdummy");
            binaryOpLirLineList.add(checkZero.toString());
			if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
			{
				String result = String.valueOf(Integer.parseInt(leftOp) / Integer.parseInt(rightOp));
				target.add(result);
			}
			else
			{
				if (!CompileTimeData.isRegName(leftOp))
				{
					String reg = RegisterFactory.allocateRegister();
					BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, reg);
					binaryOpLirLineList.add(mem2reg.toString());
					leftOp = reg;
				}
				inst = new BinaryInstruction(LirBinaryOps.DIV, rightOp, leftOp);
				binaryOpLirLineList.add(inst.toString());
				target.add(leftOp);
			}
			if (CompileTimeData.isRegName(rightOp))
				RegisterFactory.freeRegister(rightOp);
			break;
		case MOD:
			if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
			{
				String result = String.valueOf(Integer.parseInt(leftOp) % Integer.parseInt(rightOp));
				target.add(result);
			}
			else
			{
				if (!CompileTimeData.isRegName(leftOp))
				{
					String reg = RegisterFactory.allocateRegister();
					BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, reg);
					binaryOpLirLineList.add(mem2reg.toString());
					leftOp = reg;
				}
				inst = new BinaryInstruction(LirBinaryOps.MOD, rightOp, leftOp);
				binaryOpLirLineList.add(inst.toString());
				target.add(leftOp);
			}
			if (CompileTimeData.isRegName(rightOp))
				RegisterFactory.freeRegister(rightOp);
			break;
		case MULTIPLY:
			if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
			{
				String result = String.valueOf(Integer.parseInt(leftOp) * Integer.parseInt(rightOp));
				target.add(result);
			}
			else
			{
				if (!CompileTimeData.isRegName(leftOp))
				{
					String reg = RegisterFactory.allocateRegister();
					BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, reg);
					binaryOpLirLineList.add(mem2reg.toString());
					leftOp = reg;
				}
				inst = new BinaryInstruction(LirBinaryOps.MUL, rightOp, leftOp);
				binaryOpLirLineList.add(inst.toString());
				target.add(leftOp);
			}
			if (CompileTimeData.isRegName(rightOp))
				RegisterFactory.freeRegister(rightOp);
			break;
		case MINUS:
			if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
			{
				String result = String.valueOf(Integer.parseInt(leftOp) - Integer.parseInt(rightOp));
				target.add(result);
			}
			else
			{
				if (!CompileTimeData.isRegName(leftOp))
				{
					String reg = RegisterFactory.allocateRegister();
					BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, reg);
					binaryOpLirLineList.add(mem2reg.toString());
					leftOp = reg;
				}
				inst = new BinaryInstruction(LirBinaryOps.SUB, rightOp, leftOp);
				binaryOpLirLineList.add(inst.toString());
				target.add(leftOp);
			}
			if (CompileTimeData.isRegName(rightOp))
				RegisterFactory.freeRegister(rightOp);
			break;
		case PLUS:
			if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
			{
				String result = String.valueOf(Integer.parseInt(leftOp) + Integer.parseInt(rightOp));
				target.add(result);
			}
			else
			{
				if (leftOperator.getAssignedType() == TypesTable.getPrimitiveEntry(PrimitiveTypeEnum.STRING))
				{ // string concatenation
					String concat = "__stringCat(" + leftOp + "," + rightOp + ")";

					String reg;
					if (CompileTimeData.isRegName(leftOp))
						reg = leftOp;
					else if (CompileTimeData.isRegName(rightOp))
						reg = rightOp;
					else
						reg = RegisterFactory.allocateRegister();
					inst = new BinaryInstruction(LirBinaryOps.LIBRARY, concat, reg);
					binaryOpLirLineList.add(inst.toString());
					target.add(reg);
					
					if (CompileTimeData.isRegName(rightOp) && !reg.equals(rightOp))
						RegisterFactory.freeRegister(rightOp);
				}
				else
				{
					if (!CompileTimeData.isRegName(leftOp))
					{
						String reg = RegisterFactory.allocateRegister();
						BinaryInstruction mem2reg = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, reg);
						binaryOpLirLineList.add(mem2reg.toString());
						leftOp = reg;
					}
					inst = new BinaryInstruction(LirBinaryOps.ADD, rightOp, leftOp);
					binaryOpLirLineList.add(inst.toString());
					target.add(leftOp);
					if (CompileTimeData.isRegName(rightOp))
						RegisterFactory.freeRegister(rightOp);
				}
			}
			break;
		default:
			break;
		}
		return binaryOpLirLineList;
	}

	@Override
	public List<String> visit(LogicalBinaryOp binaryOp, List<String> target) throws Exception {
		if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
			target.remove(0);
		List<String> binaryOpLirLineList = new LinkedList<String>();
		Expression firstOperand = binaryOp.getFirstOperand();
		List<String> firstOperandRegisters = new LinkedList<String>();
		firstOperandRegisters.add(VAL_OPTMZ);
		List<String> firstOperandTR = firstOperand.accept(this, firstOperandRegisters);
		binaryOpLirLineList.addAll(firstOperandTR);
		Expression secondOperand = binaryOp.getSecondOperand();
		List<String> secondOperandRegisters = new LinkedList<String>();
		secondOperandRegisters.add(VAL_OPTMZ);
		List<String> secondOperandTR = secondOperand.accept(this, secondOperandRegisters);
		binaryOpLirLineList.addAll(secondOperandTR);

		String firstOperandResult = firstOperandRegisters.get(0);
		String secondOperandResult = secondOperandRegisters.get(0);
		Label trueLable = null;
		LirUnaryOps jumpOp = null;
		Label endLabel = new Label("_end_binaryOp_label_" + binaryOp.getLine());
		BinaryInstruction checkFirstOperand;
		UnaryInstruction jumpTrue;

		String tempRegister;
		if (!CompileTimeData.isRegName(firstOperandResult)) {
			tempRegister = RegisterFactory.allocateRegister();
			BinaryInstruction initTempRegister = new BinaryInstruction(LirBinaryOps.MOVE, firstOperandResult, tempRegister);
			binaryOpLirLineList.add(initTempRegister.toString());
		}
		else {
			tempRegister = firstOperandResult;
		}

		switch (binaryOp.getOperator()) {
		case LAND:
			checkFirstOperand = new BinaryInstruction(LirBinaryOps.COMPARE, "0", tempRegister);
			binaryOpLirLineList.add(checkFirstOperand.toString());
			jumpTrue = new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel);
			binaryOpLirLineList.add(jumpTrue.toString());
			BinaryInstruction and = new BinaryInstruction(LirBinaryOps.AND, secondOperandResult, tempRegister);
			binaryOpLirLineList.add(and.toString());
			binaryOpLirLineList.add(endLabel.toString());
			target.add(tempRegister);
			break;
		case LOR:
			checkFirstOperand = new BinaryInstruction(LirBinaryOps.COMPARE, "1", tempRegister);
			binaryOpLirLineList.add(checkFirstOperand.toString());
			jumpTrue = new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel);
			binaryOpLirLineList.add(jumpTrue.toString());
			BinaryInstruction or = new BinaryInstruction(LirBinaryOps.OR, secondOperandResult, tempRegister);
			binaryOpLirLineList.add(or.toString());
			binaryOpLirLineList.add(endLabel.toString());
			target.add(tempRegister);
			break;
		case LT:
			trueLable = new Label("_L_label_" + binaryOp.getLine());
			jumpOp = LirUnaryOps.JUMPL;
		case LTE:
			if (trueLable == null) {
				trueLable = new Label("_LE_label_" + binaryOp.getLine());
				jumpOp = LirUnaryOps.JUMPLE;
			}
		case GT:
			if (trueLable == null) {
				trueLable = new Label("_G_label_" + binaryOp.getLine());
				jumpOp = LirUnaryOps.JUMPG;
			}
		case GTE:
			if (trueLable == null) {
				trueLable = new Label("_GE_label_" + binaryOp.getLine());
				jumpOp = LirUnaryOps.JUMPGE;
			}
		case EQUAL:
			if (trueLable == null) {
				trueLable = new Label("_EQ_label_" + binaryOp.getLine());
				jumpOp = LirUnaryOps.JUMPTRUE;
			}
		case NEQUAL:
			if (trueLable == null) {
				trueLable = new Label("_NEQ_label_" + binaryOp.getLine());
				jumpOp = LirUnaryOps.JUMPFALSE;
			}

			BinaryInstruction compareOperands = new BinaryInstruction(LirBinaryOps.COMPARE, secondOperandResult, tempRegister);
			binaryOpLirLineList.add(compareOperands.toString());
			UnaryInstruction jumpOpLabel = new UnaryInstruction(jumpOp, trueLable);
			binaryOpLirLineList.add(jumpOpLabel.toString());
			BinaryInstruction setFalse = new BinaryInstruction(LirBinaryOps.MOVE, "0", tempRegister);
			binaryOpLirLineList.add(setFalse.toString());
			UnaryInstruction jump = new UnaryInstruction(LirUnaryOps.JUMP, endLabel);
			binaryOpLirLineList.add(jump.toString());
			binaryOpLirLineList.add(trueLable.toString());
			BinaryInstruction setTrue = new BinaryInstruction(LirBinaryOps.MOVE, "1", tempRegister);
			binaryOpLirLineList.add(setTrue.toString());
			binaryOpLirLineList.add(endLabel.toString());
			target.add(tempRegister);
			break;
		default:
			System.out.println("something's wrong");
		}

		return binaryOpLirLineList;
	}

	@Override
	public List<String> visit(MathUnaryOp unaryOp, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> unaryOpBlock = new LinkedList<String>();
		Expression operandExpression = unaryOp.getOperand();
		List<String> operandRegisters = new ArrayList<String>();
		operandRegisters.add(VAL_OPTMZ);
		List<String> unaryOpTR = operandExpression.accept(this, operandRegisters);
		unaryOpBlock.addAll(unaryOpTR);
		String targetOperandRegister = operandRegisters.get(0);
		UnaryInstruction unaryOpInstruction;

		if (CompileTimeData.isImmediate(targetOperandRegister)) {
			int operandAsInt = Integer.parseInt(targetOperandRegister);
			targetRegisters.add(String.valueOf(-1*operandAsInt));
		}
		else if (CompileTimeData.isRegName(targetOperandRegister)) {
			unaryOpInstruction = new UnaryInstruction(LirUnaryOps.NEG, targetOperandRegister);
			unaryOpBlock.add(unaryOpInstruction.toString());
			targetRegisters.add(targetOperandRegister);
		}
		else if (CompileTimeData.isMemory(targetOperandRegister)) {
			String targetUnaryOpRegister = RegisterFactory.allocateRegister();
			BinaryInstruction getMemory = new BinaryInstruction(LirBinaryOps.MOVE, targetOperandRegister, targetUnaryOpRegister);
			unaryOpBlock.add(getMemory.toString());
			unaryOpInstruction = new UnaryInstruction(LirUnaryOps.NEG, targetUnaryOpRegister);
			unaryOpBlock.add(unaryOpInstruction.toString());
			targetRegisters.add(targetUnaryOpRegister);
		}

		return unaryOpBlock;
	}

	@Override
	public List<String> visit(LogicalUnaryOp unaryOp, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> unaryOpBlock = new LinkedList<String>();
		Expression operandExpression = unaryOp.getOperand();
		List<String> operandRegisters = new ArrayList<String>();
		List<String> unaryOpTR = operandExpression.accept(this, operandRegisters);
		unaryOpBlock.addAll(unaryOpTR);
		String targetOperandRegister = operandRegisters.get(0);
		UnaryInstruction unaryOpInstruction;

		if (CompileTimeData.isImmediate(targetOperandRegister)) {
			int operandAsInt = Integer.parseInt(targetOperandRegister);
			targetRegisters.add(String.valueOf((operandAsInt + 1) % 2));
		}
		else if (CompileTimeData.isRegName(targetOperandRegister)) {
			unaryOpInstruction = new UnaryInstruction(LirUnaryOps.NOT, targetOperandRegister);
			unaryOpBlock.add(unaryOpInstruction.toString());
			targetRegisters.add(targetOperandRegister);
		}
		else if (CompileTimeData.isMemory(targetOperandRegister)) {
			String targetUnaryOpRegister = RegisterFactory.allocateRegister();
			BinaryInstruction getMemory = new BinaryInstruction(LirBinaryOps.MOVE, targetOperandRegister, targetUnaryOpRegister);
			unaryOpBlock.add(getMemory.toString());
			unaryOpInstruction = new UnaryInstruction(LirUnaryOps.NOT, targetUnaryOpRegister);
			unaryOpBlock.add(unaryOpInstruction.toString());
			targetRegisters.add(targetUnaryOpRegister);
		}

		return unaryOpBlock;
	}

	@Override
	public List<String> visit(Literal literal, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		String value = null;
		switch (literal.getType()) {
		case TRUE:
			value = "1";
			break;
		case FALSE:
			value = "0";
			break;
		case STRING:
			String strLiteralVal = literal.getValue().toString();
			value = CompileTimeData.addStringLiteralGetSymbol(strLiteralVal);
			break;
		case INTEGER:
			value = String.valueOf(literal.getValue());
			break;
		case NULL:
			value = "0";
			break;
		}
		targetRegisters.add(value);
		return new LinkedList<String>();
	}

	@Override
	public List<String> visit(ExpressionBlock expressionBlock, List<String> targetRegisters) throws Exception {
		if (!targetRegisters.isEmpty() && targetRegisters.get(0).equals(VAL_OPTMZ))
			targetRegisters.remove(0);
		List<String> expressionBlockBlock = new LinkedList<String>();
		Expression expression = expressionBlock.getExpression();
		List<String> expressionRegisters = new LinkedList<String>();
		List<String> expressionTR = expression.accept(this, expressionRegisters);
		expressionBlockBlock.addAll(expressionTR);
		targetRegisters.add(expressionRegisters.get(0));
		return expressionBlockBlock;
	}
}
