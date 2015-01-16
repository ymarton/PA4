package IC.lir;

import java.util.Stack;

public class RegisterFactory {

	private static Stack<String> deadRegistersPool = new Stack<String>();
	private static int headRegOfFreeRegsBlock = 0;
	
	private RegisterFactory() {}
	
    public static String allocateRegister() {
    	if (!RegisterFactory.deadRegistersPool.isEmpty())
    		return RegisterFactory.deadRegistersPool.pop();
    	
        String allocatedReg = "R" + RegisterFactory.headRegOfFreeRegsBlock;
        RegisterFactory.headRegOfFreeRegsBlock++;
        return allocatedReg;
    }
    
    public static void freeRegister(String deadReg)
    {
    	deadRegistersPool.add(deadReg);
    }

}
