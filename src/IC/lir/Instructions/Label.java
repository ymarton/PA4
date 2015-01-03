package IC.lir.Instructions;

public class Label extends LirLine {

    private String labelValue;

    public Label(String label) {
        this.labelValue = label;
    }

    public String getLabelValue() {
        return labelValue;
    }

    @Override
    public String toString() {
        return labelValue + ":\n";
    }

}
