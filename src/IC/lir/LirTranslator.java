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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LirTranslator implements PropagatingVisitor<String,List<LirLine>> {

	
	/*
    private Map<String,ClassLayout> classLayouts;
    private List<LirLine> stringLiterals = new LinkedList<LirLine>();
    */
    private int stringCounter = 1;
    private String currentClass;
    private ScopeChecker scopeChecker = new ScopeChecker();
    private TypeChecking typeChecking = new TypeChecking();
    /*private List<DispatchTable> dispatchTableList = new LinkedList<DispatchTable>();*/

    @Override
    public List<LirLine> visit(Program program, String target) throws Exception {
    	DispacthTableBuilder.init(program);
    	DispacthTableBuilder.buildClassLayouts();
    	
        List<LirLine> instructionList = new LinkedList<LirLine>();
        for (ICClass icClass : program.getClasses()) {
            instructionList.addAll(icClass.accept(this, null));
        }
        instructionList.addAll(0, CompileTimeData.getDispatchTables());
        instructionList.addAll(0, CompileTimeData.getStringLiterals());
        return instructionList;
    }

    @Override
  //TODO: check if _ic_main need to be at the end of the lir program
    public List<LirLine> visit(ICClass icClass, String target) throws Exception {
    	String className = icClass.getName();
    	ClassLayout classLayout = CompileTimeData.getClassLayout(className);
    	DispatchTable classDT = new DispatchTable(className, classLayout);
    	CompileTimeData.addDispatchTable(classDT);
    	
        /*dispatchTableList.add(new DispatchTable(icClass.getName(), classLayouts.get(icClass.getName())));*/
    	
        List<LirLine> classInstructions = new LinkedList<LirLine>();
        currentClass = icClass.getName();
        for (Method method : icClass.getMethods()) {
        	List<LirLine> methodInstructions = new LinkedList<LirLine>();
        	LirLine methodLabel;
        	
        	if ( (method instanceof StaticMethod) && (method.getName().equals("main")) )
        		methodLabel = new Label("_ic_" + method.getName());
        	else
        		methodLabel = new Label("_" + currentClass + "_" + method.getName());
        	
        	if (!classInstructions.isEmpty())
        		methodInstructions.add(new BlankLine());
        	methodInstructions.add(methodLabel);
        	methodInstructions.addAll(method.accept(this, null));
        	classInstructions.addAll(methodInstructions);
        }
        return classInstructions;
    }

    @Override
    public List<LirLine> visit(Field field, String target) throws Exception 
    { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<LirLine> visit(VirtualMethod method, String target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
    	
        List<LirLine> methodInstructions = new LinkedList<LirLine>();
        /* 
        LirLine methodLabel = new Label("_" + currentClass + "_" + method.getName());
        methodInstructions.add(methodLabel);
         */
        for (Statement statement : method.getStatements()) {
        	methodInstructions.addAll(statement.accept(this, null));
        }
       
        return methodInstructions;
    }

    //TODO: intentionally didnt add a methodLabel? now adding for each method at icclass level...
    @Override
    public List<LirLine> visit(StaticMethod method, String target) throws Exception {
        //TODO: set all arguments to new registers and somehow pass that information
        //on second thought, is it done automatically on each statement?
        List<LirLine> methodInstructions = new LinkedList<LirLine>();
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
    public List<LirLine> visit(LibraryMethod method, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<LirLine> visit(Formal formal, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<LirLine> visit(PrimitiveType type, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

    @Override
    public List<LirLine> visit(UserType type, String target) throws Exception { throw new Exception("shouldn't be invoked..."); }

//    @Override
//    public List<LirLine> visit(Assignment assignment, RegisterFactory factory) throws Exception {
//        List<LirLine> assignmentLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(Assignment assignment, RegisterFactory factory) throws Exception {
        List<LirLine> assignmentLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(CallStatement callStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(Return returnStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(If ifStatement, RegisterFactory factory) throws Exception {
        List<LirLine> ifLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(While whileStatement, RegisterFactory factory) throws Exception {
        List<LirLine> whileLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(Break breakStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(Continue continueStatement, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(StatementsBlock statementsBlock, RegisterFactory factory) throws Exception {
        List<LirLine> statementBlockLirLineList = new LinkedList<LirLine>();
        for (Statement statement : statementsBlock.getStatements()) {
            statementBlockLirLineList.addAll(statement.accept(this, factory));
        }
        return statementBlockLirLineList;
    }

    @Override
    public List<LirLine> visit(LocalVariable localVariable, RegisterFactory factory) throws Exception {
        List<LirLine> localVariableLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(VariableLocation location, RegisterFactory factory) throws Exception {
        List<LirLine> variableLocationLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(ArrayLocation location, RegisterFactory factory) throws Exception {
        //TR[e1[e2]]
        //  R1:=TR[e1]
        //  R2:=TR[e2]
        //  MoveArray R1[R2],R3
        List<LirLine> arrayLocationLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(StaticCall call, RegisterFactory factory) throws Exception {
        return null;
    }

    @Override
    public List<LirLine> visit(VirtualCall call, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(This thisExpression, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(NewClass newClass, RegisterFactory factory) throws Exception {
        List<LirLine> newClassLirLineList = new LinkedList<LirLine>();
        int objectSize = classLayouts.get(newClass.getName()).getFieldToOffsetSize() + 1;
        objectSize *= 4;
        String register = factory.allocateRegister();
        newClassLirLineList.add(new BinaryInstruction(LirBinaryOps.LIBRARY, "__alocateObject(" + objectSize + ")", register));
        factory.setTargetRegister(register);
        newClassLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVEFIELD, "_" + newClass.getName() + "_DV", register + ".0"));
        return newClassLirLineList;
    }

    @Override
    public List<LirLine> visit(NewArray newArray, RegisterFactory factory) throws Exception {
        List<LirLine> newArrayLirLineList = new LinkedList<LirLine>();
        newArrayLirLineList.addAll(newArray.getSize().accept(this, factory));
        String sizeRegister = factory.getTargetRegister1();
        newArrayLirLineList.add((new BinaryInstruction(LirBinaryOps.LIBRARY, "__allocateArray(" + sizeRegister + ")", sizeRegister)));
        factory.resetTargetRegisters();
        factory.setTargetRegister(sizeRegister);
        return newArrayLirLineList;
    }

    @Override
    public List<LirLine> visit(Length length, RegisterFactory factory) throws Exception { return null; }

    @Override
    public List<LirLine> visit(MathBinaryOp binaryOp, RegisterFactory factory) throws Exception {
        /*
            TR[e1 OP e2]:
                R1:=TR[e1]
                R2:=TR[e2]
                R3:=R1 OP R2
         */
        List<LirLine> binaryOpLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(LogicalBinaryOp binaryOp, RegisterFactory factory) throws Exception {
        List<LirLine> binaryOpLirLineList = new LinkedList<LirLine>();
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
    public List<LirLine> visit(MathUnaryOp unaryOp, RegisterFactory factory) throws Exception {
        List<LirLine> unaryOpLirLineList = new LinkedList<LirLine>();
        unaryOpLirLineList.addAll(unaryOp.getOperand().accept(this, factory));
        unaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.NEG, factory.getTargetRegister1()));
        return unaryOpLirLineList;
    }

    @Override
    public List<LirLine> visit(LogicalUnaryOp unaryOp, RegisterFactory factory) throws Exception {
        List<LirLine> unaryOpLirLineList = new LinkedList<LirLine>();
        unaryOpLirLineList.addAll(unaryOp.getOperand().accept(this, factory));
        unaryOpLirLineList.add(new UnaryInstruction(LirUnaryOps.NOT, factory.getTargetRegister1()));
        return unaryOpLirLineList;
    }

    @Override
    public List<LirLine> visit(Literal literal, RegisterFactory factory) throws Exception {
        List<LirLine> literalLirLineList = new LinkedList<LirLine>();
        String register = factory.allocateRegister();
        LiteralTypes literalType = literal.getType();
        String value;
        switch (literalType) {
            case TRUE:
                value = "1";
                break;
            case FALSE:
                value = "0";
                break;
            case STRING:
                value = literal.getValue().toString();
                stringLiterals.add(new stringLiteral("str" + stringCounter, value));
                value = "str" + stringCounter;
                stringCounter++;
                break;
            default:
                value = literal.getValue().toString();
        }
        literalLirLineList.add(new BinaryInstruction(LirBinaryOps.MOVE, value, register));
        factory.setTargetRegister(register);
        return literalLirLineList;
    }

    @Override
    public List<LirLine> visit(ExpressionBlock expressionBlock, RegisterFactory factory) throws Exception {
        List<LirLine> expressionBlockLirLineList = new LinkedList<LirLine>();
        expressionBlockLirLineList.addAll(expressionBlock.getExpression().accept(this, factory));
        return expressionBlockLirLineList;
    }
}
