package IC.lir.Instructions;

public class stringLiteral extends LirLine {

    private String name;
    private String value;

    public stringLiteral(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name + ": " + value + "\n";
    }
    
}
