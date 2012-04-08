package com.badlogic.crux;

import java.util.ArrayList;
import java.util.List;

public interface AstNode {
	/**
	 * A Program consists of {@link ProgramPart} instances, e.g.
	 * variable/function/type declarations and statements. The
	 * order of parts is preserved.
	 * @author mzechner
	 *
	 */
	public static class Program implements AstNode {
		public List<ProgramPart> parts = new ArrayList<ProgramPart>();
	}
	
	/**
	 * A ProgramPart is a variable/function/type declaration 
	 * This is a tagging interface for ast nodes
	 * representing the four concepts mentioned above.
	 * @author mzechner
	 *
	 */
	public interface ProgramPart extends AstNode {
	}
	
	/**
	 * A variable declaration specifies the number of pointer
	 * indirections, type and identifier for a variable. Optionally
	 * it specifies the initializer.
	 * @author mzechner
	 *
	 */
	public static class VariableDeclaration implements ProgramPart {
		public int pointerIndirections;
		public String type;
		public String identifier;
		public Initializer initializer;
	}
	
	/**
	 * An initializer is either an expression or a data section.
	 * @author mzechner
	 *
	 */
	public static class Initializer implements AstNode {
		// FIXME
	}

	/**
	 * A structure definition consists of a list of {@link VariableDeclaration}
	 * instances defining the fields of the structure.
	 * @author mzechner
	 *
	 */
	public static class StructureDeclaration implements ProgramPart {
		public String name;
		public List<VariableDeclaration> fields = new ArrayList<VariableDeclaration>();
	}
	
	/**
	 * A function definition specifies the function name, an optional
	 * argument list and an optional return value type. 
	 * @author mzechner
	 *
	 */
	public static class FunctionDefinition implements ProgramPart {
		public String identifier;
		public List<VariableDeclaration> arguments = new ArrayList<VariableDeclaration>();
		public List<VariableDeclaration> localVars = new ArrayList<VariableDeclaration>();
		public List<Statement> statements = new ArrayList<Statement>();
		public ReturnType returnType;
	}
	
	public static class ReturnType implements AstNode {
		public int pointerIndirections;
		public String type;
	}
	
	/**
	 * A statement is an assignment, function call, if, while or return control structure.
	 * @author mzechner
	 *
	 */
	public interface Statement extends AstNode {
	}
	
	public static class Assignment implements Statement {
	}
	
	public static class FunctionCall implements Statement {
	}
	
	public static class IfStatement implements Statement {
	}
	
	public static class WhileStatement implements Statement {
	}
	
	public static class ReturnStatement implements Statement {
	}
}