package IC.lir.Instructions;

public class UnaryInstruction extends LirLine{

    protected String operand;
    protected LirUnaryOps operator;

    public UnaryInstruction(LirUnaryOps unaryOp, String operand) {
        this.operator = unaryOp;
        this.operand = operand;
    }

    public UnaryInstruction(LirUnaryOps unaryOp, Label label) {
        this.operator = unaryOp;
        this.operand = label.getLabelValue();
    }

    @Override
    public String toString() {
        return operator.getDescription() + " " + operand + "\n";
    }
}
