package IC.lir.Instructions;

import IC.lir.ClassLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DispatchTable {

    private String name;
    private List<String> methodList = new LinkedList<String>();
    private Map<String, String> methodDeclatingMap;
    
    public DispatchTable(String className, ClassLayout layout) {
        this.name = "_DV_" + className;
        this.methodDeclatingMap = layout.getDeclaringMap();
        methodList.addAll(layout.getMethodNames());
    }

    @Override
    public String toString() {
    	String declaringClass;
        String ret = name + ": [";
        for (String method : methodList) {
        	declaringClass = this.methodDeclatingMap.get(method);
            ret += "_" + declaringClass + "_" + method + ",";
        }
        if (!methodList.isEmpty()) {
            ret = ret.substring(0, ret.length()-1);
        }
        ret += "]\n";
        return ret;
    }
}
