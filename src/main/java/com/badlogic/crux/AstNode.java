package com.badlogic.crux;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.crux.AstNode.LValue;

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
	
	enum Type {
		Number,
		Struct,
		Function
	}
	
	public static class TypeDefinition implements AstNode {
		public int references;
		public Type type;
		public String name;
		public AnonymousFunctionSignature funcSig;
	}
	
	/**
	 * A variable declaration specifies the number of pointer
	 * indirections, type and identifier for a variable. Optionally
	 * it specifies the initializer.
	 * @author mzechner
	 *
	 */
	public static class VariableDeclaration implements ProgramPart, Statement {
		public String identifier;
		public TypeDefinition typeDef = new TypeDefinition();
		public Initializer initializer;
	}
	
	public static class FunctionSignature implements AstNode {
		public List<VariableDeclaration> arguments = new ArrayList<VariableDeclaration>();
		public List<Statement> statements = new ArrayList<Statement>();
		public ReturnType returnType;
	}
	
	public static class AnonymousFunctionSignature implements AstNode {
		public List<TypeDefinition> argumentTypes = new ArrayList<TypeDefinition>();
		public ReturnType returnType;
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
		public FunctionSignature signature = new FunctionSignature();
		public List<Statement> statements = new ArrayList<Statement>();
	}
	
	public static class ReturnType implements AstNode {
		public TypeDefinition typeDef = new TypeDefinition();
	}
	
	/**
	 * A statement is an assignment, function call, if, while or return control structure.
	 * @author mzechner
	 *
	 */
	public interface Statement extends AstNode {
	}
	
	public static class Assignment implements Statement {
		public LValue lvalue;
		public Expression rvalue;
	}
	
	public abstract class LValue implements Factor {
		public LValue indirection;
	}
	
	public static class Dereference extends LValue {
		public LValue lvalue;
		public LValue indirection;
	}
	
	public static class OffsetDereference extends LValue {
		public String identifier;
		public Expression offsetExpression; // can be null
	}
	
	public static class FunctionCall implements Statement {
		public LValue lvalue;
		public List<Expression> arguments = new ArrayList<Expression>();
	}
	
	public static class IfStatement implements Statement {
		public Expression condition;
		public List<Statement> trueStatements = new ArrayList<Statement>();
		public List<Statement> elseStatements = new ArrayList<Statement>();
	}
	
	public static class WhileStatement implements Statement {
		public Expression condition;
		public List<Statement> statements = new ArrayList<Statement>();
	}
	
	public static class BreakStatement implements Statement {
	}
	
	public static class ReturnStatement implements Statement {
		Expression expression; // can be null
	}
	
	public static interface Expression extends AstNode {
	}
	
	public static class ComparisonExpression implements Expression {
		public enum Comparator {
			Less,
			LessEqual,
			Equal,
			NotEqual,
			GreaterEqual,
			Greater
		}
		Comparator operator;
		Expression left;
		Expression right;
	}
	
	public static class LogicalExpression implements Expression {
		public enum LogicalOperator {
			And,
			Or
		}
		LogicalOperator operator;
		Expression left;
		Expression right;
	}
	
	public static class AdditiveExpression implements Expression {
		public enum AdditiveOperator {
			Add,
			Subtract
		}
		AdditiveOperator operator;
		Expression left;
		Expression right;
	}
	
	public static class MultiplicativeExpression implements Expression {
		public enum MultiplicativeOperator {
			Multiply,
			Divide,
			SignedDivide,
			Modulo
		}
		MultiplicativeOperator operator;
		Expression left;
		Expression right;
	}
	
	public static class BinaryExpression implements Expression {
		public enum BinaryOperator {
			ShiftLeft,
			ShiftRight,
			Or,
			And,
			Xor
		}
		BinaryOperator operator;
		Expression left;
		Expression right;
	}
	
	public static class UnaryExpression implements Expression {
		public enum UnaryOperator {
			Negate,
			Not
		}
		UnaryOperator operator;
		Expression factor;
	}
	
	public interface Factor extends Expression {
	}
	
	public static class Number implements Factor {
		public String value;
	}
	
	public static class Literal implements Factor {
		public String value;
	}
	
	public static class Value implements Factor {
		public int references;
	}
	
	public static class FunctionReturnValue extends Value {
		public FunctionCall functionCall;
	}
	
	public static class RValue extends Value {
		public LValue lvalue;
	}
}