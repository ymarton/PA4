package IC.lir;

import IC.AST.ASTNode;
import IC.AST.Field;
import IC.AST.ICClass;
import IC.AST.Method;
import IC.AST.Program;
import IC.AST.VirtualMethod;
import IC.Semantic.ClassNode;
import IC.Semantic.ClassesGraph;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

public class DispacthTableBuilder {

	private static ASTNode root = null;
	private static HashMap<String, ClassNode> allClassesNodes = null;
	private static HashMap<String, ASTNode> allClassesASTs = new LinkedHashMap<String, ASTNode>();
	
	private DispacthTableBuilder() {}
	
	public static void init(ASTNode rootPtr)
	{
		root = rootPtr;
	}
	
	private static void initUsefulDataStructs()
	{
		allClassesNodes = ClassesGraph.getAllVertices();

		for (ICClass icclass : ((Program)root).getClasses())
		{
			allClassesASTs.put(icclass.getName(), icclass);
		}
	}
	public static void buildClassLayouts()
	{
		initUsefulDataStructs();
		
		// BFS order will guarantee that if class being build has superclass, then superclass already built
		Queue<String> buildQueue = new LinkedList<String>();
		for (Entry<String, ClassNode> vertexEntry : allClassesNodes.entrySet())
		{
			if (vertexEntry.getValue().getSuperName() == null)
				buildQueue.add(vertexEntry.getKey());
		}
		while (!buildQueue.isEmpty())
		{
			String nextClass = buildQueue.poll();
			ClassNode nextClassNode = allClassesNodes.get(nextClass);
			String superClass = nextClassNode.getSuperName();
			
			Map<String, Integer> classFieldsMap = new LinkedHashMap<String, Integer>();
			Map<String, Integer> classMethodsMap = new LinkedHashMap<String, Integer>();
			Map<String, String> classMethodsDeclaringMap = new LinkedHashMap<String, String>();
			
			if (superClass != null)
			{
				ClassLayout superLayout = CompileTimeData.getClassLayout(superClass);
				Map<String, Integer> superFieldsMap = superLayout.getFieldsMap();
				Map<String, Integer> superMethodsMap = superLayout.getMethodsMap();
				Map<String, String> superMethodsDeclaringMap = superLayout.getDeclaringMap();
				
				classFieldsMap.putAll(superFieldsMap);
				classMethodsMap.putAll(superMethodsMap);
				classMethodsDeclaringMap.putAll(superMethodsDeclaringMap);
			}
			
			ICClass classASTnode = (ICClass) allClassesASTs.get(nextClass);
			
			for (Field field : classASTnode.getFields())
			{
				classFieldsMap.put(field.getName(), classFieldsMap.size()+1);
			}
			
			for (Method method : classASTnode.getMethods())
			{
				String methodName = method.getName();
				boolean isVirtual = (method instanceof VirtualMethod);
				if (isVirtual && !classMethodsMap.containsKey(methodName))
					classMethodsMap.put(methodName, classMethodsMap.size());
				classMethodsDeclaringMap.put(methodName, nextClass);
			}
			
			ClassLayout classLayout = new ClassLayout(classMethodsMap, classFieldsMap, classMethodsDeclaringMap);
			CompileTimeData.addClassLayout(nextClass, classLayout);

			Set<String> directChildren = nextClassNode.getNeighboursList().keySet();
			for (String child : directChildren)
			{
					buildQueue.add(child);
			}
		}
	}
	/*
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
                if (method.getName().equals("main")) {
                    continue;
                }
                methodToOffset.put(method.getName(), methodOffset);
                methodOffset++;
            }

            ClassLayout layout = new ClassLayout(methodToOffset, fieldToOffset);
            classLayouts.put(icClass.getName(), layout);
        }

        return classLayouts;
    }
    */
}
