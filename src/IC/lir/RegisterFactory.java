package IC.lir;

public class RegisterFactory {

    private int registerCounter;
    private String targetRegister1;
    private String targetRegister2;
    private String targetRegister3;

    public RegisterFactory() {
        registerCounter = 0;
    }

    public String allocateRegister() {
        registerCounter++;
        return "R" + registerCounter;
    }

    public void freeRegister() {
        registerCounter--;
    }

    public void setTargetRegister(String targetRegister) {
        if (targetRegister1 == null)
            targetRegister1 = targetRegister;
        else
            if (targetRegister2 == null)
                targetRegister2 = targetRegister;
            else
                targetRegister3 = targetRegister;
    }

    public String getTargetRegister1() {
        return targetRegister1;
    }

    public String getTargetRegister2() {
        return targetRegister2;
    }

    public String getTargetRegister3() {
        return targetRegister3;
    }

    public void resetTargetRegisters() {
        targetRegister1 = targetRegister2 = null;
    }
}
