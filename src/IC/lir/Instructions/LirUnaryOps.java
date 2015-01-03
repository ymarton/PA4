package IC.lir.Instructions;

public enum LirUnaryOps {

    NOT("Not"),
    INC("Inc"),
    DEC("Dec"),
    NEG("Neg"),
    JUMP("Jump"),
    JUMPTRUE("JumpTrue"),
    JUMPFALSE("JumpFalse"),
    JUMPG("JumpG"),
    JUMPGE("JumpGE"),
    JUMPL("JumpL"),
    JUMPLE("JumpLE"),
    RETURN("Return");

    private String description;

    private LirUnaryOps(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
