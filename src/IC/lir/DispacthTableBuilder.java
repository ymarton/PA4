package IC.lir;

import IC.AST.Field;
import IC.AST.ICClass;
import IC.AST.Method;
import IC.AST.Program;

import java.util.HashMap;
import java.util.Map;

public class DispacthTableBuilder {

    public static Map<String,ClassLayout> build(Program program) {
        Map<String,ClassLayout> classLayouts = new HashMap<String, ClassLayout>();
        for (ICClass icClass : program.getClasses()) {

            Map<String,Integer> fieldToOffset = new HashMap<String,Integer>();
            int fieldOffset = 1;
            for (Field field : icClass.getFields()) {
                fieldToOffset.put(field.getName(), fieldOffset);
                fieldOffset++;
            }

            Map<String,Integer> methodToOffset = new HashMap<String, Integer>();
            int methodOffset = 0;
            for (Method method : icClass.getMethods()) {
                methodToOffset.put(method.getName(), methodOffset);
                methodOffset++;
            }

            ClassLayout layout = new ClassLayout(methodToOffset, fieldToOffset);
            classLayouts.put(icClass.getName(), layout);
        }

        return classLayouts;
    }
}
