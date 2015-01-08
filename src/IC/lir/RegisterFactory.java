package IC.lir;

import java.util.Stack;

public class RegisterFactory {

	private static Stack<String> deadRegistersPool = new Stack<String>();
	private static int headRegOfFreeRegsBlock = 0;
	
	private RegisterFactory() {}
	/*
    private int registerCounter;
    private String targetRegister1;
    private String targetRegister2;
    private String targetRegister3;
	
    public RegisterFactory() {
        registerCounter = 0;
    }
	*/
    public static String allocateRegister() {
    	if (!RegisterFactory.deadRegistersPool.isEmpty())
    		return RegisterFactory.deadRegistersPool.pop();
    	
        String allocatedReg = "R" + RegisterFactory.headRegOfFreeRegsBlock;
        RegisterFactory.headRegOfFreeRegsBlock++;
        return allocatedReg;
    }

    public static void freeStackOfDeadRegisters(Stack<String> deadStack)
    {
    	for (String deadReg : deadStack) {
			deadRegistersPool.add(deadReg);
		}
    }
    
    public static Stack<String> newLocalRegStack()
    {
    	return new Stack<String>();
    }
    
    /*
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
    */
}
