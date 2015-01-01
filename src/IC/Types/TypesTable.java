package IC.Types;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import IC.DataTypes;
import IC.AST.Formal;
import IC.AST.PrimitiveType;
import IC.AST.Type;
import IC.AST.UserType;

public class TypesTable {


	static private String programFilename = "";
	public static void setProgFilename(String filename)
	{
		TypesTable.programFilename = filename;
	}
	private static int counter = 1;

	private static LinkedHashSet<MethodTypeEntry> methods = new LinkedHashSet<MethodTypeEntry>();
	private static LinkedHashSet<ArrayTypeEntry> arrays = new LinkedHashSet<ArrayTypeEntry>();
	private static LinkedHashMap<String, ClassTypeEntry> classes = new LinkedHashMap<String, ClassTypeEntry>();

	private static EnumMap<PrimitiveTypeEnum, PrimitiveTypeEntry> primitives = new EnumMap<PrimitiveTypeEnum, PrimitiveTypeEntry>(PrimitiveTypeEnum.class);

	private TypesTable() {}

	static
	{
		generatePrimitives();
		generateEntriesOfMain();
	}

	private static void updateSuperForClasses()
	{
		for (ClassTypeEntry c : classes.values())
		{
			if (c.getSuperName() != null)
			{
				ClassTypeEntry superClass = classes.get(c.getSuperName());
				if (superClass != null)
					c.setSuperID(superClass.getEntryID());
			}
		}
	}

	public static void addMethodEntry(MethodTypeEntry methodEntry)
	{
		if (methods.add(methodEntry))
			counter++;
	}

	public static void addClassEntry(ClassTypeEntry classEntry)
	{
		if (!classes.containsKey(classEntry.getName()))
		{
			classes.put(classEntry.getName(), classEntry);
			counter++;
		}
	}

	/* hasnt anything to do with the input program of the compiler */
	private static void generatePrimitives()
	{
		for (PrimitiveTypeEnum a : PrimitiveTypeEnum.values())
		{
			PrimitiveTypeEntry primitive = new PrimitiveTypeEntry(a, counter);
			primitives.put(a, primitive);
			counter++;
		}
	}

	/* generate types for main func - again not related to user input */
	private static void generateEntriesOfMain()
	{
		PrimitiveType argsArrType =  new PrimitiveType(0, DataTypes.STRING);
		argsArrType.incrementDimension(); // now == args[] aka dim == 1
		ArrayTypeEntry argsEntry = new ArrayTypeEntry(argsArrType, counter);
		TypesTable.addArrayEntry(argsEntry);

		Formal argsAsFormal = new Formal(argsArrType, "args");
		List<Formal> formals = new LinkedList<Formal>();
		formals.add(argsAsFormal);
		MethodTypeEntry funcEntry = new MethodTypeEntry(new PrimitiveType(0, DataTypes.VOID), formals, counter);
		TypesTable.addMethodEntry(funcEntry);
	}
	public static void addArrayEntry(ArrayTypeEntry arrayEntry)
	{
		if (arrays.add(arrayEntry))
			counter++;
	}

	public static String niceOut()
	{
		updateSuperForClasses();
		String out = "Type Table: " + TypesTable.programFilename;
		LinkedList<AbstractEntryTypeTable> allEntries = new LinkedList<AbstractEntryTypeTable>();
		allEntries.addAll(primitives.values());
		allEntries.addAll(classes.values());
		allEntries.addAll(arrays);
		allEntries.addAll(methods);

		for (AbstractEntryTypeTable entry : allEntries)
		{
			if (!out.isEmpty())
				out += "\n    ";
			out += entry.getformattedEntry();
		}
		return out;
	}

	public static AbstractEntryTypeTable addSubDimsAndGetMyTypeEntry(Type type)
	{

		boolean isPrimitiveBased =  (type instanceof PrimitiveType) ? true : false;

		int typeDim = type.getDimension();

		if (typeDim < 1)
		{
			if (isPrimitiveBased)
			{
				PrimitiveType t = (PrimitiveType)type;
				PrimitiveTypeEnum entryEnum = PrimitiveTypeEntry.dataTypeToEnum(t.getInternalEnum());
				return new PrimitiveTypeEntry(entryEnum);
			}
			else
			{
				UserType t = (UserType)type;
				return new ClassTypeEntry(t.getName());
			}
		}

		Type typeSingleDim;
		ArrayTypeEntry arrayEntry = null;
		for (int dim = 1; dim <= typeDim; dim++)
		{
			if (isPrimitiveBased)
				typeSingleDim = PrimitiveType.cloneTypeButResetDimTo1((PrimitiveType)type);
			else
				typeSingleDim = UserType.cloneTypeButResetDimTo1((UserType)type);

			for (int j = 1; j < dim; j++)
				typeSingleDim.incrementDimension();

			arrayEntry = new ArrayTypeEntry(typeSingleDim, TypesTable.getCounter());
			TypesTable.addArrayEntry(arrayEntry);
		}
		return new ArrayTypeEntry(type);
	}

	public static PrimitiveTypeEntry getPrimitiveEntry(PrimitiveTypeEnum key)
	{
		return primitives.get(key);
	}

	public static int getCounter()
	{
		return counter;
	}

	public static AbstractEntryTypeTable getTypeEntryForType(Type type)
	{
		boolean isPrimitiveBased =  (type instanceof PrimitiveType) ? true : false;

		int typeDim = type.getDimension();

		if (typeDim < 1)
		{
			if (isPrimitiveBased)
			{
				PrimitiveType t = (PrimitiveType)type;
				PrimitiveTypeEnum entryEnum = PrimitiveTypeEntry.dataTypeToEnum(t.getInternalEnum());
				return new PrimitiveTypeEntry(entryEnum);
			}
			else
			{
				UserType t = (UserType)type;
				return new ClassTypeEntry(t.getName());
			}
		}

		return new ArrayTypeEntry(type);
	}
}