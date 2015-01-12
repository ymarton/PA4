package IC.lir;

import IC.AST.*;
import IC.AST.Return;
import IC.AST.StaticCall;
import IC.AST.VirtualCall;
import IC.Types.ClassTypeEntry;
import IC.lir.Instructions.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LirTranslator implements PropagatingVisitor<List<String>,List<String>> {
	
	public static final String VAL_OPTMZ = "";
	
    private String currentClass;
    private Label currentWhileLabel;
    private Label currentEndWhileLabel;

    @Override
    public List<String> visit(Program program, List<String> target) throws Exception {
    	DispacthTableBuilder.init(program);
    	DispacthTableBuilder.buildClassLayouts();
    	
        List<String> instructionList = new LinkedList<String>();
        for (ICClass icClass : program.getClasses()) {
            instructionList.addAll(icClass.accept(this, null));
        }
        instructionList.addAll(0, CompileTimeData.getDispatchTables());
        instructionList.addAll(0, CompileTimeData.getStringLiterals());
        return instructionList;
    }

    @Override
  //TODO: check if _ic_main need to be at the end of the lir program
    public List<String> visit(ICClass icClass, List<String> target) throws Exception {
    	String className = icClass.getName();
    	ClassLayout classLayout = CompileTimeData.getClassLayout(className);
    	DispatchTable classDT = new DispatchTable(className, classLayout);
    	CompileTimeData.addDispatchTable(classDT);
    	
        List<String> classInstructions = new LinkedList<String>();
        currentClass = icClass.getName();
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
        	classInstructions.addAll(methodInstructions);
        }

        return classInstructions;
    }

    @Override
    public List<String> visit(Field field, List<String> target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<String> visit(VirtualMethod method, List<String> target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
        List<String> methodInstructions = new LinkedList<String>();
        for (Statement statement : method.getStatements()) {
        	methodInstructions.addAll(statement.accept(this, null));
        }
        return methodInstructions;
    }

    @Override
    public List<String> visit(StaticMethod method, List<String> target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
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
        List<String> locationTR = assignExpr.accept(this, locationRegs);
        
        assignmentLirLineList.addAll(assignTR);
        assignmentLirLineList.addAll(locationTR);

        BinaryInstruction assignInst;
        String assignOp = assignRegs.get(0);
        String locationOp;
        if (location instanceof ArrayLocation)
        {
        	// locationRegs = {base, index}, index can be immediate/reg
        	// assignRegs = immediate/reg/memory
        	locationOp = locationRegs.get(0) + "[" + locationRegs.get(1)+ "]";
        	
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

            if (CompileTimeData.isRegName(locationRegs.get(0)))
                RegisterFactory.freeRegister(locationRegs.get(0));

            if (CompileTimeData.isRegName(locationRegs.get(1)))
                RegisterFactory.freeRegister(locationRegs.get(1));
        }

        else if (location instanceof VariableLocation) {
            //locationRegs = {memory}/{reg,immediate}
            //assignRegs = (immediate/reg/memory)
            if (!((VariableLocation) location).isExternal()) {//location = var
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
        List<String> callRegisters = new LinkedList<String>();
        List<String> callStatementTR = call.accept(this, callRegisters);
        callStatementBlock.addAll(callStatementTR);
        return callStatementBlock;
    }

    @Override
    public List<String> visit(Return returnStatement, List<String> targetRegisters) throws Exception {
        List<String> returnStatementBlock = new LinkedList<String>();
        if (returnStatement.hasValue()) {
            Expression returnValue = returnStatement.getValue();
            List<String> returnStatementRegisters = new LinkedList<String>(); //add hack?
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
        BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, "0", conditionRegisters.get(0));
        ifStatementBlock.add(checkCondition.toString());
        RegisterFactory.freeRegister(conditionRegisters.get(0));

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

        BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, "0", conditionRegisters.get(0));
        whileStatementBlock.add(checkCondition.toString());
        UnaryInstruction falseCondition = new UnaryInstruction(LirUnaryOps.JUMPTRUE, currentEndWhileLabel);
        whileStatementBlock.add(falseCondition.toString());
        Statement operation = whileStatement.getOperation();
        List<String> operationTR = operation.accept(this, null);
        whileStatementBlock.addAll(operationTR);
        UnaryInstruction startOver = new UnaryInstruction(LirUnaryOps.JUMP, currentWhileLabel);
        whileStatementBlock.add(startOver.toString());
        whileStatementBlock.add(currentEndWhileLabel.toString());
        RegisterFactory.freeRegister(conditionRegisters.get(0));
        return whileStatementBlock;
    }

    @Override
    public List<String> visit(Break breakStatement, List<String> targetRegisters) throws Exception {
        List<String> breakStatementBlock = new LinkedList<String>();
        breakStatementBlock.add(currentEndWhileLabel.toString());
        return new LinkedList<String>();
    }

    @Override
    public List<String> visit(Continue continueStatement, List<String> targetRegisters) throws Exception {
        List<String> continueStatementBlock = new LinkedList<String>();
        continueStatementBlock.add(currentWhileLabel.toString());
        return new LinkedList<String>();
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
        if (!location.isExternal()) {
            if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
            	target.remove(0);
            target.add(location.getName());
        }
        else {
            List<String> baseRegs = new ArrayList<String>();
            baseRegs.add(VAL_OPTMZ);
            Expression baseExpr = location.getLocation();
            List<String> baseLirInstructions = baseExpr.accept(this, baseRegs);
            
            //baseRegs contains memory/reg, both cases had to return target in this form {regX, offset} / {RegX}
            variableLocationLirLineList.addAll(baseLirInstructions);
            String exprOp = baseRegs.get(0);
            if (!CompileTimeData.isRegName(exprOp)) // memory
            {
            	String exprReg = RegisterFactory.allocateRegister();
                BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, exprOp, exprReg);
                variableLocationLirLineList.add(getMem.toString());
                exprOp = exprReg;
            }
            // exprReg == Reg with expr
            
            // need to calculate the offset
            String className = ((ClassTypeEntry)baseExpr.getAssignedType()).getName();
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
        
        List<String> indexRegs = new ArrayList<String>();
        indexRegs.add(VAL_OPTMZ);
        Expression indexExpr = location.getIndex();
        List<String> indexLirInstructions = indexExpr.accept(this, indexRegs);
        arrayLocationLirLineList.addAll(indexLirInstructions);
        String indexOp = indexRegs.get(0);
        
        if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
        {
        	target.remove(0);
        	BinaryInstruction optmz = new BinaryInstruction(LirBinaryOps.MOVEARRAY, arrayOp + "[" + indexOp + "]", arrayOp);
        	arrayLirInstructions.add(optmz.toString());
        	target.add(arrayOp);
        	
        	if (CompileTimeData.isRegName(indexOp))
        		RegisterFactory.freeRegister(indexOp);
        }
        else
        {
        	target.add(arrayOp);
        	target.add(indexOp);
        }
        return arrayLirInstructions;
        /*
        //baseRegs contains memory/reg, both cases had to return target in this form {regX, offset} / {RegX}
        variableLocationLirLineList.addAll(baseLirInstructions);
        String exprOp = baseRegs.get(0);
        
        factory.resetTargetRegisters();
        arrayLocationLirLineList.addAll(location.getArray().accept(this, factory));
        arrayLocationLirLineList.addAll(location.getIndex().accept(this, factory));
        String register1 = factory.getTargetRegister1();
        String register2 = factory.getTargetRegister2();
        arrayLocationLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVEARRAY, register1 + "["+ register2 + "]", register1));
        factory.freeRegister();
        return arrayLocationLirLineList;
        */
    }

    @Override
    public List<String> visit(StaticCall call, RegisterFactory factory) throws Exception {
        return null;
    }

    @Override
    public List<String> visit(VirtualCall call, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(This thisExpression, List<String> targetRegisters) throws Exception {
        targetRegisters.add("this"); //that's it???
        return new LinkedList<String>();
    }

    @Override
    public List<String> visit(NewClass newClass, List<String> targetRegisters) throws Exception {
        List<String> newClassBlock = new LinkedList<String>();
        int objectSize = CompileTimeData.getClassLayout(newClass.getName()).getFieldToOffsetSize() + 1;
        objectSize *= 4;
        String targetRegister = RegisterFactory.allocateRegister();
        BinaryInstruction allocateObject = new BinaryInstruction(LirBinaryOps.LIBRARY, "__alocateObject(" + objectSize + ")", targetRegister);
        newClassBlock.add(allocateObject.toString());
        BinaryInstruction addDVPTR = new BinaryInstruction(LirBinaryOps.MOVEFIELD, "_DV_" + newClass.getName(), targetRegister + ".0");
        newClassBlock.add(addDVPTR.toString());
        targetRegisters.add(targetRegister);
        return newClassBlock;
    }

    @Override
    public List<String> visit(NewArray newArray, List<String> targetRegisters) throws Exception {
        List<String> newArrayBlock = new LinkedList<String>();
        Expression sizeExpression = newArray.getSize();
        List<String> sizeExpressionRegisters = new LinkedList<String>();
        List<String> sizeExpressionTR = sizeExpression.accept(this, sizeExpressionRegisters); //add hack?
        newArrayBlock.addAll(sizeExpressionTR);
        String sizeRegister = sizeExpressionRegisters.get(0);
        BinaryInstruction allocateArray;

        if (CompileTimeData.isRegName(sizeRegister)) {
            allocateArray =  new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeRegister + ")", sizeRegister); //is it okay to use the same register to store the result?
            newArrayBlock.add(allocateArray.toString());
            targetRegisters.add(sizeRegister);
        }
        else if (CompileTimeData.isMemory(sizeRegister) || CompileTimeData.isImmediate(sizeRegister)) {
            String targetRegister = RegisterFactory.allocateRegister();
            allocateArray =  new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeRegister + ")", targetRegister);
            newArrayBlock.add(allocateArray.toString());
            targetRegisters.add(targetRegister);
        }

        return newArrayBlock;
    }

    @Override
    public List<String> visit(Length length, List<String> targetRegisters) throws Exception {
        List<String> lengthBlock = new LinkedList<String>();
        Expression arrayExpression = length.getArray();
        List<String> arrayExpressionRegisters = new LinkedList<String>();
        arrayExpressionRegisters.add(VAL_OPTMZ);
        List<String> arrayExpressionTR = arrayExpression.accept(this, arrayExpressionRegisters); //add ArrayLocation hack?
        lengthBlock.addAll(arrayExpressionTR);
        String arrayAndTargetRegister = arrayExpressionRegisters.get(0);
        BinaryInstruction lengthInstruction = new BinaryInstruction(LirBinaryOps.ARRAYLENGTH, arrayAndTargetRegister, arrayAndTargetRegister); //is it okay to use the same register to store the result?
        lengthBlock.add(lengthInstruction.toString());
        targetRegisters.add(arrayAndTargetRegister);
        return lengthBlock;
    }

    @Override
    public List<String> visit(MathBinaryOp binaryOp, List<String> target) throws Exception {
    	
    	/* should be in a setAndGet of MathBinaryOp... */
    	
    	if (!target.isEmpty() && target.get(0).equals(VAL_OPTMZ))
    		target.remove(0);
    	Expression leftOperator = binaryOp.getFirstOperand();
    	Expression rightOperator = binaryOp.getSecondOperand();
    	int leftWeight = leftOperator.setAndGetRegWeight();
    	int rightWeight = rightOperator.setAndGetRegWeight();
    	/* should be in a setAndGet of MathBinaryOp... */

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
					String temp = RegisterFactory.allocateRegister();
	                BinaryInstruction getMem = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp);
	                binaryOpLirLineList.add(getMem.toString());
					inst = new BinaryInstruction(LirBinaryOps.DIV, reg, temp);
					binaryOpLirLineList.add(inst.toString());
					BinaryInstruction optmz = new BinaryInstruction(LirBinaryOps.MOVE, temp, reg);
					binaryOpLirLineList.add(optmz.toString());
					RegisterFactory.freeRegister(temp);
					target.add(reg);
					break;
				case MINUS:
					String temp2 = RegisterFactory.allocateRegister();
	                BinaryInstruction getMem2 = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp2);
	                binaryOpLirLineList.add(getMem2.toString());
					inst = new BinaryInstruction(LirBinaryOps.SUB, reg, temp2);
					binaryOpLirLineList.add(inst.toString());
					BinaryInstruction optmz2 = new BinaryInstruction(LirBinaryOps.MOVE, temp2, reg);
					binaryOpLirLineList.add(optmz2.toString());
					RegisterFactory.freeRegister(temp2);
					target.add(reg);
					break;
				case MOD:
					String temp3 = RegisterFactory.allocateRegister();
	                BinaryInstruction getMem3 = new BinaryInstruction(LirBinaryOps.MOVE, leftOp, temp3);
	                binaryOpLirLineList.add(getMem3.toString());
					inst = new BinaryInstruction(LirBinaryOps.MOD, reg, temp3);
					binaryOpLirLineList.add(inst.toString());
					BinaryInstruction optmz3 = new BinaryInstruction(LirBinaryOps.MOVE, temp3, reg);
					binaryOpLirLineList.add(optmz3.toString());
					RegisterFactory.freeRegister(temp3);
					target.add(reg);
					break;
				case MULTIPLY:
					inst = new BinaryInstruction(LirBinaryOps.MUL, leftOp, reg);
					binaryOpLirLineList.add(inst.toString());
					target.add(reg);
					break;
				case PLUS:
					inst = new BinaryInstruction(LirBinaryOps.ADD, leftOp, reg);
					binaryOpLirLineList.add(inst.toString());
					target.add(reg);
					break;
				default:
					break;
				}
        	}
        }
        else // have side effects OR left heavier or same than "random"  => left is first
    	{
        	leftOpInstructions = leftOperator.accept(this, leftOpRegs);
    		binaryOpLirLineList.addAll(leftOpInstructions);
    		String leftOp = leftOpRegs.get(0);
    		
    		rightOpInstructions = rightOperator.accept(this, rightOpRegs);
    		binaryOpLirLineList.addAll(rightOpInstructions);
    		String rightOp = rightOpRegs.get(0);
    		
    		BinaryInstruction inst;
    		switch (binaryOp.getOperator()) {
			case DIVIDE:
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
				break;
			case PLUS:
				if (CompileTimeData.isImmediate(rightOp) && CompileTimeData.isImmediate(leftOp))
				{
					String result = String.valueOf(Integer.parseInt(leftOp) + Integer.parseInt(rightOp));
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
					inst = new BinaryInstruction(LirBinaryOps.ADD, rightOp, leftOp);
					binaryOpLirLineList.add(inst.toString());
					target.add(leftOp);
				}
				break;
			default:
				break;
			}
    	}
        return binaryOpLirLineList;
    }

    @Override
    public List<String> visit(LogicalBinaryOp binaryOp, RegisterFactory factory) throws Exception {
        List<String> binaryOpLirLineList = new LinkedList<String>();
        binaryOpLirLineList.addAll(binaryOp.getFirstOperand().accept(this, factory));
        binaryOpLirLineList.addAll(binaryOp.getSecondOperand().accept(this, factory));
        Label trueLable = null;
        LirUnaryOps jumpOp = null;
        Label endLabel = new Label("_end_binaryOp_label_" + binaryOp.getLine());
        switch (binaryOp.getOperator()) {
            case LAND:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.COMPARE, "0", factory.getTargetRegister1()));
                binaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel));
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.AND, factory.getTargetRegister2(), factory.getTargetRegister1()));
                binaryOpLirLineList.add(endLabel);
                break;
            case LOR:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.COMPARE, "1", factory.getTargetRegister1()));
                Label endOrLabel = new Label("_end_and_label_" + binaryOp.getLine());
                binaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, endOrLabel));
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.AND, factory.getTargetRegister2(), factory.getTargetRegister1()));
                binaryOpLirLineList.add(endOrLabel);
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
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.COMPARE, factory.getTargetRegister2(), factory.getTargetRegister1()));
                binaryOpLirLineList.add(new UnaryInstruction(jumpOp, trueLable));
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, "0", factory.getTargetRegister1()));
                binaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMP, endLabel));
                binaryOpLirLineList.add(trueLable);
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, "1", factory.getTargetRegister1()));
                binaryOpLirLineList.add(endLabel);
                break;
            default:
                System.out.println("something's wrong");
        }
        factory.freeRegister();
        return binaryOpLirLineList;
    }

    @Override
    public List<String> visit(MathUnaryOp unaryOp, List<String> targetRegisters) throws Exception {
        List<String> unaryOpBlock = new LinkedList<String>();
        Expression operandExpression = unaryOp.getOperand();
        List<String> operandRegisters = new ArrayList<String>();
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
        List<String> expressionBlockBlock = new LinkedList<String>();
        Expression expression = expressionBlock.getExpression();
        List<String> expressionRegisters = new LinkedList<String>();
        List<String> expressionTR = expression.accept(this, expressionRegisters);
        expressionBlockBlock.addAll(expressionTR);
        targetRegisters.add(expressionRegisters.get(0));
        return expressionBlockBlock;
    }
}
