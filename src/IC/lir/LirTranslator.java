package IC.lir;

import IC.AST.*;
import IC.AST.Return;
import IC.AST.StaticCall;
import IC.AST.VirtualCall;
import IC.lir.Instructions.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LirTranslator implements PropagatingVisitor<List<String>,List<String>> {

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
    public List<String> visit(CallStatement callStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(Return returnStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(If ifStatement, List<String> targetRegisters) throws Exception {
        List<String> ifStatementBlock = new LinkedList<String>();
        Expression condition = ifStatement.getCondition();
        List<String> conditionRegisters = new LinkedList<String>();
        List<String> conditionTR = condition.accept(this, conditionRegisters);
        ifStatementBlock.addAll(conditionTR);
        BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, conditionRegisters.get(0), "0");
        ifStatementBlock.add(checkCondition.toString());
        RegisterFactory.freeRegister(conditionRegisters.get(0)); //is it okay here?

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
        List<String> conditionRegisters = new LinkedList<String>();
        List<String> conditionTR = condition.accept(this, conditionRegisters);
        whileStatementBlock.addAll(conditionTR);

        BinaryInstruction checkCondition = new BinaryInstruction(LirBinaryOps.COMPARE, conditionRegisters.get(0), "0");
        whileStatementBlock.add(checkCondition.toString());
        RegisterFactory.freeRegister(conditionRegisters.get(0)); //is it okay here?
        UnaryInstruction falseCondition = new UnaryInstruction(LirUnaryOps.JUMPTRUE, currentEndWhileLabel);
        whileStatementBlock.add(falseCondition.toString());
        Statement operation = whileStatement.getOperation();
        List<String> operationTR = operation.accept(this, null);
        whileStatementBlock.addAll(operationTR);
        UnaryInstruction startOver = new UnaryInstruction(LirUnaryOps.JUMP, currentWhileLabel);
        whileStatementBlock.add(startOver.toString());
        whileStatementBlock.add(currentEndWhileLabel.toString());
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
    public List<String> visit(LocalVariable localVariable, RegisterFactory factory) throws Exception {
        List<String> localVariableLirLineList = new LinkedList<String>();
        if (localVariable.hasInitValue()) {
            localVariableLirLineList.addAll(localVariable.getInitValue().accept(this, factory));
            localVariableLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, factory.getTargetRegister1(), localVariable.getName()));
            factory.freeRegister();
        }
        else {
            //do something???
        }
        return localVariableLirLineList;
    }

    @Override
    public List<String> visit(VariableLocation location, RegisterFactory factory) throws Exception {
        List<String> variableLocationLirLineList = new LinkedList<String>();
        if (!location.isExternal()) {
            String register = factory.allocateRegister();
            variableLocationLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, location.getName(), register));
            factory.setTargetRegister(register);
        }
        else {
            //TODO: complete
            variableLocationLirLineList.addAll(location.getLocation().accept(this, factory));
        }
        return variableLocationLirLineList;
    }

    @Override
    public List<String> visit(ArrayLocation location, RegisterFactory factory) throws Exception {
        //TR[e1[e2]]
        //  R1:=TR[e1]
        //  R2:=TR[e2]
        //  MoveArray R1[R2],R3
        List<String> arrayLocationLirLineList = new LinkedList<String>();
        factory.resetTargetRegisters();
        arrayLocationLirLineList.addAll(location.getArray().accept(this, factory));
        arrayLocationLirLineList.addAll(location.getIndex().accept(this, factory));
        String register1 = factory.getTargetRegister1();
        String register2 = factory.getTargetRegister2();
        arrayLocationLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVEARRAY, register1 + "["+ register2 + "]", register1));
        factory.freeRegister();
        return arrayLocationLirLineList;
    }

    @Override
    public List<String> visit(StaticCall call, RegisterFactory factory) throws Exception {
        return null;
    }

    @Override
    public List<String> visit(VirtualCall call, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(This thisExpression, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(NewClass newClass, RegisterFactory factory) throws Exception {
        List<String> newClassLirLineList = new LinkedList<String>();
        int objectSize = classLayouts.get(newClass.getName()).getFieldToOffsetSize() + 1;
        objectSize *= 4;
        String register = factory.allocateRegister();
        newClassLirLineList.add(new BinaryInstruction(LirBinaryOps.LIBRARY, "__alocateObject(" + objectSize + ")", register));
        factory.setTargetRegister(register);
        newClassLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVEFIELD, "_" + newClass.getName() + "_DV", register + ".0"));
        return newClassLirLineList;
    }

    @Override
    public List<String> visit(NewArray newArray, RegisterFactory factory) throws Exception {
        List<String> newArrayLirLineList = new LinkedList<String>();
        newArrayLirLineList.addAll(newArray.getSize().accept(this, factory));
        String sizeRegister = factory.getTargetRegister1();
        newArrayLirLineList.add((new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeRegister + ")", sizeRegister)));
        factory.resetTargetRegisters();
        factory.setTargetRegister(sizeRegister);
        return newArrayLirLineList;
    }

    @Override
    public List<String> visit(Length length, List<String> targetRegisters) throws Exception {
        List<String> lengthBlock = new LinkedList<String>();
        Expression arrayExpression = length.getArray();
        List<String> arrayExpressionRegisters = new LinkedList<String>();
        List<String> arrayExpressionTR = arrayExpression.accept(this, arrayExpressionRegisters); //add ArrayLocation hack
        lengthBlock.addAll(arrayExpressionTR);
        String targetRegister = RegisterFactory.allocateRegister();
        BinaryInstruction lengthInstruction = new BinaryInstruction(LirBinaryOps.ARRAYLENGTH, arrayExpressionRegisters.get(0), targetRegister);
        lengthBlock.add(lengthInstruction.toString());
        targetRegisters.add(targetRegister);
        return lengthBlock;
    }

    @Override
    public List<String> visit(MathBinaryOp binaryOp, RegisterFactory factory) throws Exception {
        /*
            TR[e1 OP e2]:
                R1:=TR[e1]
                R2:=TR[e2]
                R3:=R1 OP R2
         */
        List<String> binaryOpLirLineList = new LinkedList<String>();
        binaryOpLirLineList.addAll(binaryOp.getFirstOperand().accept(this, factory));
        binaryOpLirLineList.addAll(binaryOp.getSecondOperand().accept(this, factory));
        switch (binaryOp.getOperator()) {
            case PLUS:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.ADD, factory.getTargetRegister2(), factory.getTargetRegister1()));
                break;
            case MINUS:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.SUB, factory.getTargetRegister2(), factory.getTargetRegister1()));
                break;
            case MULTIPLY:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.MUL, factory.getTargetRegister2(), factory.getTargetRegister1()));
                break;
            case DIVIDE:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.DIV, factory.getTargetRegister2(), factory.getTargetRegister1()));
                break;
            case MOD:
                binaryOpLirLineList.add(new BinaryInstruction(LirBinaryOps.MOD, factory.getTargetRegister2(), factory.getTargetRegister1()));
                break;
        }
        factory.freeRegister();
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
