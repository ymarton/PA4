package IC.Semantic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClassesGraph {
	private static HashMap<String, ClassNode> vertices = new LinkedHashMap<String, ClassNode>();
	private static HashMap<String, String> derivedSuperRelation = new LinkedHashMap<String, String>();

	private static int marksCounter = 0;
	
	private ClassesGraph() {}

	/**
	 * add (derived,super) edge for edges table - later will be used to build real graph edges
	 * @param superName - super class name
	 * @param derivedName - derived class name
	 */
	public static void addSuperDerivedRelation(String superName, String derivedName)
	{
		ClassesGraph.derivedSuperRelation.put(derivedName, superName);
	}
	
	/**
	 * @return true/false if succeed building all edges and all the used vertices(classes) are defined
	 * (success for some edge (u,v) if both u & v have classDecl-s)
	 */
	private static boolean buildEdges()
	{
		for (Map.Entry<String, String> relation : derivedSuperRelation.entrySet())
		{
			ClassNode  derivedNode = getClassNode(relation.getKey());
			ClassNode superNode = getClassNode(relation.getValue());
			if (superNode == null)
				return false;
			else if (derivedNode == null)
				return false;
			else
				superNode.addSubclass(derivedNode);
		}
		return true;
	}
	
	/**
	 * add classNode vertex to graph
	 * @param classNode
	 */
	public static void addVertex(ClassNode classNode)
	{
		ClassesGraph.vertices.put(classNode.getName(), classNode);
	}

	/**
	 * @param classname - class name
	 * @return classNode vertex that represents the class classname
	 */
	public static ClassNode getClassNode(String classname)
	{
		return vertices.get(classname);
	}
	
	/**
	 * @param derivedClassName - class name, candidate to be the derived
	 * @param superClassName - class name, candidate to be a super (class can have many supers...)
	 * @return true/false
	 */
	public static  boolean isDerivedAndSuper(String derivedClassName, String superClassName)
	{
		String parentClassName = findMyParentClassName(derivedClassName);
		while (parentClassName != null)
		{
			if (parentClassName.equals(superClassName))
				return true;
			parentClassName = findMyParentClassName(parentClassName);
		}
		return false;
	}
	
	/**
	 * @param classname - class name
	 * @return class name that its directly derived from (like a extends b => func(a)=b), or null if there's no such class
	 */
	public static String findMyParentClassName(String classname)
	{
		ClassNode classNode = ClassesGraph.getClassNode(classname);
		if (classNode != null)
		{
			ClassNode parentNode = ClassesGraph.getClassNode(classNode.getSuperName());
			if (parentNode != null)
				return parentNode.getName();
		}
		return null;
	}
	
	/**
	 * @return true/false if all class names used in "extend" had also ClassDec & if the graph is a forest (tree/group of trees)
	 * implemented algorithm is DFS
	 */
	public static boolean isAcyclic()
	{

		boolean buildingResult = ClassesGraph.buildEdges();
		if (!buildingResult)
			return false;

		return isAcyclic(ClassesGraph.vertices, true);
	}
	
	private static boolean isAcyclic(HashMap<String, ClassNode> neighboursList, boolean isGraphVerticesList)
	{	
		for (ClassNode classNode : neighboursList.values())
		{
			if ((isGraphVerticesList) && (classNode.getSuperName() != null))
				continue;
			if (classNode.alreadyVisited())
			{
				if (!isGraphVerticesList)
					return false;
				else
					continue;
			}
			else
			{
				classNode.visitNow();
				marksCounter++;
				if (!isAcyclic(classNode.getNeighboursList(), false))
					return false;
			}
			
		}
		
		if (isGraphVerticesList)
			if (marksCounter != vertices.size())
				return false;
		return true;
	}
}