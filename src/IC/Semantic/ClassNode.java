package IC.Semantic;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ClassNode {
	private String name;
	private String superName;
	private HashMap<String, ClassNode> subclasses;
	private boolean alreadyVisited;
	
	/**
	 * @param classname - class name
	 * @param supername - super class name
	 */
	public ClassNode(String classname, String supername)
	{
		this(classname);
		this.superName = supername;
	}
	
	/**
	 * @param classname - class name
	 */
	public ClassNode(String classname)
	{
		this.name = classname;
		this.subclasses = new LinkedHashMap<String, ClassNode>();
		this.alreadyVisited = false;
		this.superName = null;
	}
	
	/**
	 * @return super class name
	 */
	public String getSuperName()
	{
		return this.superName;
	}
	
	/**
	 * @return class name
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * @return neighbours list - each entry is a class that directly derived from this class (all x such that "x extends ClassNode.name")
	 */
	public HashMap<String, ClassNode> getNeighboursList()
	{
		return this.subclasses; 
	}
	/**
	 * mark vertex as visited - for DFS algorithm
	 */
	public void visitNow()
	{
		this.alreadyVisited = true;
	}
	
	/**
	 * @return true/false already visited
	 */
	protected boolean alreadyVisited()
	{
		return this.alreadyVisited;
	}
	
	/**
	 * @param subclassNode - add subclass (class node) to neighbors list (subclasses list / out edges)
	 */
	protected void addSubclass(ClassNode subclassNode)
	{
		this.subclasses.put(subclassNode.getName(), subclassNode);
	}
}
