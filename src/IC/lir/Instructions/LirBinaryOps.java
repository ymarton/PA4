package IC.lir.Instructions;

public enum LirBinaryOps {

    MOVE("Move"),
    MOVEARRAY("MoveArray"),
    MOVEFIELD("MoveField"),
    ARRAYLENGTH("ArrayLength"),
    ADD("Add"),
    SUB("Sub"),
    MUL("Mul"),
    DIV("Div"),
    MOD("Mod"),
    AND("And"),
    OR("Or"),
    XOR("Xor"),
    COMPARE("Compare"),
    LIBRARY("Library"),
    STATICCALL("StaticCall"),
    VIRTUALCALL("VirtualCall");

    private String description;

    private LirBinaryOps(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
