package IC.lir;

import IC.AST.*;
import IC.AST.Return;
import IC.AST.StaticCall;
import IC.AST.VirtualCall;
import IC.LiteralTypes;
import IC.Semantic.ScopeChecker;
import IC.Semantic.TypeChecking;
import IC.Symbols.Symbol;
import IC.Types.AbstractEntryTypeTable;
import IC.Types.ArrayTypeEntry;
import IC.lir.Instructions.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LirTranslator implements PropagatingVisitor<List<String>,List<String>> {

	
	/*
    private Map<String,ClassLayout> classLayouts;
    private List<String> stringLiterals = new LinkedList<String>();
    */
    private int stringCounter = 1;
    private String currentClass;
    private ScopeChecker scopeChecker = new ScopeChecker();
    private TypeChecking typeChecking = new TypeChecking();
    /*private List<DispatchTable> dispatchTableList = new LinkedList<DispatchTable>();*/

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
    	
        /*dispatchTableList.add(new DispatchTable(icClass.getName(), classLayouts.get(icClass.getName())));*/
    	
        List<String> classInstructions = new LinkedList<String>();
        currentClass = icClass.getName();
        for (Method method : icClass.getMethods()) {
        	List<String> methodInstructions = new LinkedList<String>();
        	String methodLabel;
        	
        	if ( (method instanceof StaticMethod) && (method.getName().equals("main")) )
        		methodLabel = "_ic_" + method.getName();
        	else
        		methodLabel = "_" + currentClass + "_" + method.getName();
        	
        	if (!classInstructions.isEmpty())
        		methodInstructions.add(SOMEENUM.BlankLine);
        	methodInstructions.add(methodLabel);
        	methodInstructions.addAll(method.accept(this, null));
        	classInstructions.addAll(methodInstructions);
        }
        return classInstructions;
    }

    @Override
    public List<String> visit(Field field, List<String> target) throws Exception 
    { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<String> visit(VirtualMethod method, List<String> target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
    	
        List<String> methodInstructions = new LinkedList<String>();
        /* 
        String methodLabel = new Label("_" + currentClass + "_" + method.getName());
        methodInstructions.add(methodLabel);
         */
        for (Statement statement : method.getStatements()) {
        	methodInstructions.addAll(statement.accept(this, null));
        }
       
        return methodInstructions;
    }

    //TODO: intentionally didnt add a methodLabel? now adding for each method at icclass level...
    @Override
    public List<String> visit(StaticMethod method, List<String> target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
        List<String> methodInstructions = new LinkedList<String>();
        /*methodInstructions.add(new BlankLine());*/
        
        for (Statement statement : method.getStatements()) {
        	methodInstructions.addAll(statement.accept(this, null));
        }
        /*
        if (method.getName().equals("main")) {
            methodLirLineList.add(1, new Label("_ic_" + method.getName()));
        }
        else {
            methodLirLineList.add(1, new Label("_" + currentClass + "_" + method.getName())); //TODO: check if _ic_main need to be at the end of the lir program
        }
        */
        return methodInstructions;
    }

    @Override
    public List<String> visit(LibraryMethod method, String target) throws Exception { return new LinkedList<String>(); }

    @Override
    public List<String> visit(Formal formal, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<String> visit(PrimitiveType type, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<String> visit(UserType type, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

//    @Override
//    public List<String> visit(Assignment assignment, RegisterFactory factory) throws Exception {
//        List<String> assignmentLirLineList = new LinkedList<String>();
//        factory.resetTargetRegisters();
//        assignmentLirLineList.addAll(assignment.getAssignment().accept(this, factory));
//        assignmentLirLineList.addAll(assignment.getVariable().accept(this, factory));
//        assignmentLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, factory.getTargetRegister1(), factory.getTargetRegister2())); //fix this?
//        Symbol variableSymbol = (Symbol)assignment.getVariable().accept(scopeChecker); //in order to find the variable's name. is there an easier way?
//        assignmentLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, factory.getTargetRegister2(), variableSymbol.getId()));
//        factory.freeRegister();
//        factory.freeRegister();
//        return assignmentLirLineList;
//    }

    @Override
    public List<String> visit(Assignment assignment, List<String> target) throws Exception {
        List<String> assignmentLirLineList = new LinkedList<String>();
        
        Expression assignExpr = assignment.getAssignment();
        List<String> assignRegs = new ArrayList<String>();
        List<String> assignTR = assignExpr.accept(this, assignRegs);
        /*
        List<String> assignTR = null;
        if (regsForAssignment == 0)
        	assignTR = assignExpr.accept(this, null);
        else
        {
        	assignReg = RegisterFactory.allocateRegister();
        	assignTR = assignExpr.accept(this, assignReg);
        }
        */
        Location location = assignment.getVariable();
        List<String> locationRegs = new ArrayList<String>();
        List<String> locationTR = assignExpr.accept(this, locationRegs);
        
        assignmentLirLineList.addAll(assignTR);
        assignmentLirLineList.addAll(locationTR);

        BinaryInstruction assignInst = null;
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
                locationOp = locationRegs.get(0) + "." + locationRegs.get(1)+ "]";
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
            }
        }

        if (CompileTimeData.isRegName(locationRegs.get(0)))
            RegisterFactory.freeRegister(locationRegs.get(0));

        if (CompileTimeData.isRegName(locationRegs.get(1)))
            RegisterFactory.freeRegister(locationRegs.get(1));

        return assignmentLirLineList;







        List<String> locationTR;
        String locationReg = null;
        String locationArrayPos = null;
        if ( (locationForAssign instanceof VariableLocation) && !((VariableLocation)locationForAssign).isExternal() )
        {
        	// we don't need more registration
        	locationTR = locationForAssign.accept(this, null);
        }
        else // expr.ID or expr[expr]
        {
        	locationReg = RegisterFactory.allocateRegister();
        	
        	// expr[expr]
        	if (locationForAssign instanceof ArrayLocation)
        	{
        		locationArrayPos = RegisterFactory.allocateRegister();
        		locationTR
        	}
        	locationTR = locationForAssign.accept(this, locationReg);
        }
        
        // 4 cases
        if ((locationReg != null) && (assignReg != null))
        {
        	if (locationForAssign instanceof ArrayLocation) // expr[expr]
        		
        	BinaryInstruction finalMove = new BinaryInstruction(operator, operand1, operand2)
        }
        factory.resetTargetRegisters();
        assignmentLirLineList.addAll(assignment.getAssignment().accept(this, factory));
        String register1 = factory.getTargetRegister1();
        if (assignment.getVariable() instanceof ArrayLocation) { //exp[exp] = exp
            ArrayLocation arrayLocation = (ArrayLocation) assignment.getVariable();
            assignmentLirLineList.addAll(arrayLocation.getArray().accept(this, factory));
            assignmentLirLineList.addAll(arrayLocation.getIndex().accept(this, factory));
            String register2 = factory.getTargetRegister2();
            String register3 = factory.getTargetRegister3();
            assignmentLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVEARRAY, register1, register2 + "[" + register3 + "]"));
            factory.freeRegister(); //register3
            factory.freeRegister(); //register2
        }
        else {
            VariableLocation variableLocation = (VariableLocation) assignment.getVariable();
            if (!variableLocation.isExternal()) { //exp = exp
                assignmentLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, register1, variableLocation.getName()));
            }
            else { //e.exp = exp or exp1=exp2 whereas exp1 is inherited
                //TODO: complete
            }
        }
        factory.freeRegister(); //register1
        return assignmentLirLineList;
    }

    @Override
    public List<String> visit(CallStatement callStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(Return returnStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(If ifStatement, RegisterFactory factory) throws Exception {
        List<String> ifLirLineList = new LinkedList<String>();
        ifLirLineList.addAll(ifStatement.getCondition().accept(this, factory));
        ifLirLineList.add(new BinaryInstruction(LirBinaryOps.COMPARE, "0", factory.getTargetRegister1()));
        factory.freeRegister();
        Label endLabel = new Label("_end_if_label_" + ifStatement.getLine());
        if (!ifStatement.hasElse()){
            ifLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel));
            ifLirLineList.addAll(ifStatement.getOperation().accept(this, factory));
            ifLirLineList.add(endLabel);
        }
        else {
            Label falseLabel = new Label("_false_label_" + ifStatement.getLine());
            ifLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, falseLabel));
            ifLirLineList.addAll(ifStatement.getOperation().accept(this, factory));
            ifLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel));
            ifLirLineList.add(falseLabel);
            ifLirLineList.addAll(ifStatement.getElseOperation().accept(this, factory));
            ifLirLineList.add(endLabel);
        }
        return ifLirLineList;
    }

    @Override
    public List<String> visit(While whileStatement, RegisterFactory factory) throws Exception {
        List<String> whileLirLineList = new LinkedList<String>();
        Label whileLabel = new Label("_while_label_" + whileStatement.getLine());
        Label endLabel = new Label("_end_while_label_" + whileStatement.getLine());
        whileLirLineList.add(whileLabel);
        whileLirLineList.addAll(whileStatement.getCondition().accept(this, factory));
        whileLirLineList.add(new BinaryInstruction(LirBinaryOps.COMPARE, "0", factory.getTargetRegister1()));
        factory.freeRegister();
        whileLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMPTRUE, endLabel));
        whileLirLineList.addAll(whileStatement.getOperation().accept(this, factory));
        whileLirLineList.add(new UnaryInstruction(LirUnaryOps.JUMP, whileLabel));
        whileLirLineList.add(endLabel);
        return whileLirLineList;
    }

    @Override
    public List<String> visit(Break breakStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(Continue continueStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<String> visit(StatementsBlock statementsBlock, RegisterFactory factory) throws Exception {
        List<String> statementBlockLirLineList = new LinkedList<String>();
        for (Statement statement : statementsBlock.getStatements()) {
            statementBlockLirLineList.addAll(statement.accept(this, factory));
        }
        return statementBlockLirLineList;
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
    public List<String> visit(Length length, RegisterFactory factory) throws Exception { return null; }

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
    /* mathounaryop is a unaryop is a expression, each expression derived class is implementing the .setAndGetRegWeight() abstract func
     which called *before* entering the visit, if already called - the second call will return the result without re-calc
     if got 0 - we wouldnt invoke the following at all, we can manually write the equiv. one lir line because its literal ("-1*literalval" etc)
     if >= 1 then invoke the accept after allocation register
     code illustration:
      Stack<String> usedLocaly = RegisterFactory.newLocalRegStack();
      
      int reqRegs = expr.setAndGetRegWeight()
      if (instanceof UnaryOp) && reqRegs == 0
      	do it yourself
      else
      {
      		
      		String REG = RegisterFactory.allocateRegister();
      		usedLocaly.add(REG);
      		List<String> lirblock = expr.accept(this, reg);
      		in this spot what you want is in the REG register, internal regs used at the accept should clean itself
      		....
      		...
      		.
      		....
      		asap/at most at the end you will return REG/other used to the pull with RegisterFactory.freeStackOfDeadRegisters(usedLocaly)
      		maybe its better to return each or with flag doesnt matter now
      }
    */
    public List<String> visit(MathUnaryOp unaryOp, String targetRegister) throws Exception {
        List<String> unaryOpBlock = new LinkedList<String>();
        /*Stack<String> localRegisters = RegisterFactory.newLocalRegStack();*/
        /*int numOfRequiredRegsisters = unaryOp.getOperand().setAndGetRegWeight();*/
        if (targetRegister == null) {
        	int intLiteralVal = (int) ((Literal)unaryOp.getOperand()).getValue();
        	intLiteralVal *= -1; // mathUnaryOp can be only NEG
            unaryOpBlock.add(String.valueOf(intLiteralVal)); //ugly as hell NOOOOOOOT
        }
        else {
            /*String localTargetRegister = RegisterFactory.allocateRegister();
            localRegisters.add(targetRegister); */
            List<String> operandBlock = unaryOp.getOperand().accept(this, targetRegister);
            unaryOpBlock.addAll(operandBlock);
            /*unaryOpBlock.add(new BinaryInstruction(LirBinaryOps.MOVE, localTargetRegister, targetRegister).toString());*/
            unaryOpBlock.add(new UnaryInstruction(LirUnaryOps.NEG, targetRegister).toString());
            /*RegisterFactory.freeStackOfDeadRegisters(localRegisters);*/
        }
        return unaryOpBlock;
    }

    @Override
    public List<String> visit(LogicalUnaryOp unaryOp, RegisterFactory factory) throws Exception {
        List<String> unaryOpLirLineList = new LinkedList<String>();
        unaryOpLirLineList.addAll(unaryOp.getOperand().accept(this, factory));
        unaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.NOT, factory.getTargetRegister1()));
        return unaryOpLirLineList;
    }

    //fixed 0901 TODO: null is indeed == 0?
    @Override
    public List<String> visit(Literal literal, String target) throws Exception {
        List<String> literalLirLineList = new LinkedList<String>();
        /*String register = factory.allocateRegister();
        LiteralTypes literalType = literal.getType();*/
        String value = null;
        switch (literal.getType()) {
            case TRUE:
                value = "1";
                break;
            case FALSE:
                value = "0";
                break;
            case STRING:
                String strliteralVal = literal.getValue().toString();
                value = CompileTimeData.addStringLiteralGetSymbol(strliteralVal);
                /*
                stringLiterals.add(new stringLiteral("str" + stringCounter, value));
                value = "str" + stringCounter;
                stringCounter++;
                */
                break;
            case INTEGER:
            	value = String.valueOf(literal.getValue());
            	break;
            case NULL:
            	value = "0";
                break;
        }
        /*literalLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, value, register));*/
        /*factory.setTargetRegister(register);*/
        literalLirLineList.add(value);
        return literalLirLineList;
    }

    @Override
    public List<String> visit(ExpressionBlock expressionBlock, RegisterFactory factory) throws Exception {
        List<String> expressionBlockLirLineList = new LinkedList<String>();
        expressionBlockLirLineList.addAll(expressionBlock.getExpression().accept(this, factory));
        return expressionBlockLirLineList;
    }
}
