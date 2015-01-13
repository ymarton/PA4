package IC;

import IC.AST.ASTNode;
import IC.AST.ICClass;
import IC.AST.PrettyPrinter;
import IC.AST.Program;
import IC.AST.Visitor;
import IC.Parser.Helper;
import IC.Parser.Lexer;
import IC.Parser.LibParser;
import IC.Parser.Parser;
import IC.Semantic.ClassesGraph;
import IC.Semantic.SemanticError;
import IC.Semantic.TypeChecking;
import IC.Symbols.Symbol;
import IC.Symbols.SymbolTable;
import IC.Symbols.SymbolTableBuilder;
import IC.lir.LirTranslator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import IC.Types.TypesTable;

public class Compiler {

	public static void main(String[] args) {
		Helper.fillMap();
		Symbol libSymbol = null;
		try {
			String libraryOption = getString(args, "-L.*");
			if (libraryOption != null) {
				//parse library class
				String libraryFilePath = libraryOption.substring(2);
				FileReader txtFile = new FileReader(libraryFilePath);
				LibParser parser = new LibParser(new Lexer(txtFile));
				libSymbol = parser.parse();
				ICClass libClass = (ICClass)libSymbol.value;
				System.out.println("Parsed " + libraryFilePath + " successfully!");
			}

			//parse program
			String programFilePath = args[0];
			FileReader txtFile = new FileReader(programFilePath);
			Parser parser = new Parser((new Lexer(txtFile)));
			Symbol programSymbol = parser.parse();
			System.out.println("Parsed " + programFilePath + " successfully!");
			ASTNode rootNode = (ASTNode) programSymbol.value;

			// if library was also built, combine the two ASTs to one AST - which will be pointed by rootNode
			if (libSymbol != null) {
				ICClass libClass = (ICClass)libSymbol.value;
				List<ICClass> classes = new LinkedList<ICClass>();
				classes.add(libClass);
				classes.addAll(((Program)rootNode).getClasses());
				rootNode = new Program(classes);
			}

			//build symbol table, type table
			SymbolTableBuilder symbolVisitor = new SymbolTableBuilder();
			IC.Symbols.Symbol rootSymbol = rootNode.accept(symbolVisitor, null);
			boolean declaredAndAcyclic = ClassesGraph.isAcyclic();
			if (!declaredAndAcyclic)
				throw new SemanticError("Undeclared class inheritance or cyclic class declarations (cyclic inheritance)", 0);
			SymbolTable.setClassesScopesParentsToSupers(rootSymbol.getSymTableRef());
			SymbolTable.setProgFilename(programFilePath);
			TypesTable.setProgFilename(programFilePath);
			TypeChecking typeChecking = new TypeChecking();
			rootNode.accept(typeChecking);

			if (getString(args, "-print-ast") != null)
				printAST(programFilePath, rootNode);

			if (getString(args, "-dump-symtab") != null)
				printSymbolAndTypeTable(rootSymbol);

			System.out.println();
			LirTranslator lirTranslator = new LirTranslator();
			List<String> lirProgram = rootNode.accept(lirTranslator, null);
			for (String lirLine : lirProgram) {
				System.out.print(lirLine);
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Checks if a string appears or matches a regular expression in an array
	 * @param a - a string array
	 * @param s1 - a string to look for or match
	 * @return the string found or matched, null otherwise.
	 */
	private static String getString(String[] a, String s1) {
		for (String s2 : a)
			if (s2.equals(s1) || s2.matches(s1))
				return s2;
		return null;
	}

	/**
	 * Prints an AST
	 * @param programFilePath - a string containing the path to the ic file
	 * @param rootNode - the AST root
	 */
	private static void printAST(String programFilePath, ASTNode rootNode) {
		Visitor v = new PrettyPrinter(programFilePath);
		System.out.println(rootNode.accept(v));
	}

	/**
	 * Prints the Symbol and Type tables
	 * @param rootSymbol - the root Symbol
	 */
	private static void printSymbolAndTypeTable(IC.Symbols.Symbol rootSymbol) {
		System.out.println();
		String p = rootSymbol.getSymTableRef().getNiceTableOut(rootSymbol.getKind());
		System.out.println(p);
		System.out.println(TypesTable.niceOut());
	}
}
