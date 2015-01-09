package IC.lir.Instructions;

import IC.lir.ClassLayout;

import java.util.LinkedList;
import java.util.List;

public class DispatchTable {

    private String name;
    private String className;
    private List<String> methodList = new LinkedList<String>();

    public DispatchTable(String className, ClassLayout layout) {
        this.className = className;
        this.name = "_DV_" + className;
        methodList.addAll(layout.getMethodNames());
    }

    @Override
    public String toString() {
        String ret = name + ": [";
        for (String method : methodList) {
            ret += "_" + className + "_" + method + ",";
        }
        ret = ret.substring(0, ret.length()-1);
        ret += "]\n";
        return ret;
    }
}
