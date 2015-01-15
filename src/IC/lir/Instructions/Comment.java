package IC.lir.Instructions;

public class Comment extends LirLine{

    String comment;

    public Comment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "# " + comment + "\n";
    }
}
