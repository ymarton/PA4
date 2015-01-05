package IC.lir;

import IC.AST.Field;
import IC.AST.Method;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ClassLayout {

    private Map<String,Integer> methodToOffset;
    // DVPtr = 0
    private Map<String,Integer> fieldToOffset;

    public ClassLayout(Map<String,Integer> methodToOffset, Map<String,Integer> fieldToOffset) {
        this.methodToOffset = methodToOffset;
        this.fieldToOffset = fieldToOffset;
    }
    
    public Map<String,Integer> getMethodsMap()
    {
    	return this.methodToOffset;
    }
    
    public Map<String, Integer> getFieldsMap()
    {
    	return this.fieldToOffset;
    }
    
    public int getMethodOffset(String methodName) {
        return methodToOffset.get(methodName);
    }

    public int getFieldOffset(String fieldName) {
        return fieldToOffset.get(fieldName);
    }

    public int getMethodToOffsetSize() {
        return methodToOffset.size();
    }

    public int getFieldToOffsetSize() {
        return fieldToOffset.size();
    }

    public Set<String> getMethodNames() {
        return methodToOffset.keySet();
    }

}
