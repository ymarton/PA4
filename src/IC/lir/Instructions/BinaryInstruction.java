package IC.lir.Instructions;

public class BinaryInstruction extends LirLine{

    private LirBinaryOps operator;
    private String operand1;
    private String operand2;

    public BinaryInstruction(LirBinaryOps operator, String operand1, String operand2) {
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    @Override
    public String toString() {
        return operator.getDescription() + " " + operand1 + "," + operand2 + "\n";
    }

}
