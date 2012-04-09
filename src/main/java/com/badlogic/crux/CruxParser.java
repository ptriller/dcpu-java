package com.badlogic.crux;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.crux.AstNode.AnonymousFunctionSignature;
import com.badlogic.crux.AstNode.Assignment;
import com.badlogic.crux.AstNode.BinaryExpression;
import com.badlogic.crux.AstNode.BinaryExpression.BinaryOperator;
import com.badlogic.crux.AstNode.BreakStatement;
import com.badlogic.crux.AstNode.Dereference;
import com.badlogic.crux.AstNode.Expression;
import com.badlogic.crux.AstNode.FunctionCall;
import com.badlogic.crux.AstNode.FunctionDefinition;
import com.badlogic.crux.AstNode.FunctionReturnValue;
import com.badlogic.crux.AstNode.FunctionSignature;
import com.badlogic.crux.AstNode.IfStatement;
import com.badlogic.crux.AstNode.LValue;
import com.badlogic.crux.AstNode.Literal;
import com.badlogic.crux.AstNode.Number;
import com.badlogic.crux.AstNode.OffsetDereference;
import com.badlogic.crux.AstNode.Program;
import com.badlogic.crux.AstNode.RValue;
import com.badlogic.crux.AstNode.ReturnStatement;
import com.badlogic.crux.AstNode.ReturnType;
import com.badlogic.crux.AstNode.Statement;
import com.badlogic.crux.AstNode.StructureDeclaration;
import com.badlogic.crux.AstNode.Type;
import com.badlogic.crux.AstNode.TypeDefinition;
import com.badlogic.crux.AstNode.UnaryExpression;
import com.badlogic.crux.AstNode.UnaryExpression.UnaryOperator;
import com.badlogic.crux.AstNode.VariableDeclaration;
import com.badlogic.crux.AstNode.WhileStatement;
import com.badlogic.crux.Lexer.TokenType;

/**
 * Parser for Crux, see data/crux.txt
 * @author mzechner
 *
 */
public class CruxParser extends Parser {
	Program program;

	public CruxParser (Lexer lexer) {
		super(lexer, new String[] {
			"var", "num", "struct", "func", "end", "if", "then", "else", "while", "do", "return", "break"
		});
	}
	
	public Program getProgram() {
		return program;
	}

	@Override
	public void parse () {
		program = program();
	}

	public Program program() {
		Program program = new Program();

		while(token.type != TokenType.EOF) {
			if(accept("var")) {
				program.parts.add(varDef());
			} else if("struct".equals(token.text)) {
				program.parts.add(structDecl());
			} else if("func".equals(token.text)) {
				program.parts.add(funcDef());
			} else {
				error("Expected variable definition, struct declaration or function definition");
			}
		}
		
		return program;
	}
	
	public VariableDeclaration varDef() {
		VariableDeclaration varDecl = new VariableDeclaration();
		
		while(accept(TokenType.AT)) {
			varDecl.typeDef.references++;
		}
		
		if(accept("num")) {
			expect(TokenType.IDENTIFIER);
			varDecl.typeDef.type = Type.Number;
			varDecl.identifier = lastToken.text;
		} else if(accept("func")) {
			varDecl.typeDef.type = Type.Function;
			varDecl.typeDef.name = "func";
			expect(TokenType.IDENTIFIER);
			varDecl.identifier = lastToken.text;
			varDecl.typeDef.funcSig = anonymousFuncSig();
		} else if(accept(TokenType.IDENTIFIER)) {
			varDecl.typeDef.type = Type.Struct;
			varDecl.typeDef.name = lastToken.text;
			expect(TokenType.IDENTIFIER);
			varDecl.identifier = lastToken.text;
		} else {
			error("Expected 'num' or struct name");
		}
		
		return varDecl;
	}
	
	public StructureDeclaration structDecl() {
		StructureDeclaration structDecl = new StructureDeclaration();

		expect("struct");
		expect(TokenType.IDENTIFIER);
		structDecl.name = lastToken.text;
		do {
			if(token.type == TokenType.EOF) error("expected end");
			structDecl.fields.add(varDef());
		} while(!accept("end"));
		
		return structDecl;
	}
	
	public AnonymousFunctionSignature anonymousFuncSig() {
		AnonymousFunctionSignature funcSig = new AnonymousFunctionSignature();
		// argument list
		if (accept(TokenType.L_PARA)) {
			funcSig.argumentTypes.add(typeDef());
			while (accept(TokenType.COMMA)) {
				funcSig.argumentTypes.add(typeDef());
			}
			expect(TokenType.R_PARA);
		}

		// return type
		if (accept(TokenType.COLON)) {
			funcSig.returnType = returnType();
		}
		return null;
	}
	
	public FunctionSignature funcSig() {
		FunctionSignature funcSig = new FunctionSignature();
		// argument list
		if (accept(TokenType.L_PARA)) {
			funcSig.arguments.add(varDef());
			while (accept(TokenType.COMMA)) {
				funcSig.arguments.add(varDef());
			}
			expect(TokenType.R_PARA);
		}

		// return type
		if (accept(TokenType.COLON)) {
			funcSig.returnType = returnType();
		}
		return null;
	}
	
	public FunctionDefinition funcDef() {
		FunctionDefinition funcDef = new FunctionDefinition();
		
		expect("func");
		expect(TokenType.IDENTIFIER);
		funcDef.identifier = lastToken.text;
		funcDef.signature = funcSig();
		
		// local vars and statements
		while(!accept("end")) {
			if(token.type == TokenType.EOF) error("expected end");
			funcDef.statements.add(statement());
		}
		return funcDef;
	}
	
	public TypeDefinition typeDef() {
		TypeDefinition typeDef = new TypeDefinition();
		
		while(accept(TokenType.AT)) {
			typeDef.references++;
		}
			
		if(accept("num")) {
			typeDef.type = Type.Number;
			typeDef.name = "num";
		} else if(accept("func")) {
			typeDef.type = Type.Function;
			typeDef.name = "func";
			typeDef.funcSig = anonymousFuncSig();
		} else if(accept(TokenType.IDENTIFIER)) {
			typeDef.type = Type.Struct;
			typeDef.name = lastToken.text;
		} else {
			error("Expected 'num' or struct name");
		}
		
		return typeDef;
	}
	
	public ReturnType returnType() {
		ReturnType returnType = new ReturnType();
		
		returnType.typeDef = typeDef();
		
		return returnType;
	}
	
	public Statement statement() {
		if("return".equals(token.text)) {
			return returnStatement();
		} else if("if".equals(token.text)) {
			return ifStmt();
		} else if("while".equals(token.text)) {
			return whileStmt();
		} else if(accept("break")) {
			return new BreakStatement();
		} else if(accept("var")) {
			return varDef();
		} else if(token.type == TokenType.IDENTIFIER ||
			       "[".equals(token.text)) {
			LValue lvalue = lvalue();
			if(token.type == TokenType.ASSIGN) {
				return assignment(lvalue);
			} else if(token.type == TokenType.L_PARA) {
				return functionCall(lvalue);
			}
		}
		error("Expected assignment, function call, if, while, or return");
		return null;
	}
	
	public LValue lvalue() {
		LValue lvalue = null;
		if(token.type == TokenType.L_BRACK) {
			lvalue = dereference();
		} else if(token.type == TokenType.IDENTIFIER) {
			lvalue = offsetDereference();
		} else {
			error("Expected value");
		}
		if(accept(TokenType.PERIOD)) {
			lvalue.fieldAccess = lvalue();
		}
		return lvalue;
	}
	
	public LValue offsetDereference() {
		OffsetDereference dereference = new OffsetDereference();
		expect(TokenType.IDENTIFIER);
		dereference.identifier = lastToken.text;
		if(accept(TokenType.L_BRACK)) {
			dereference.offsetExpression = expression();
			expect(TokenType.R_BRACK);
		}
		return dereference;
	}
	
	public LValue dereference() {
		Dereference dereference = new Dereference();
		expect(TokenType.L_BRACK);
		dereference.lvalue = lvalue();
		expect(TokenType.R_BRACK);
		return dereference;
	}
	
	public Assignment assignment(LValue lvalue) {
		Assignment assignment = new Assignment();
		assignment.lvalue = lvalue;
		
		expect(TokenType.ASSIGN);
		assignment.rvalue = expression();
		
		return assignment;
	}
	
	public FunctionCall functionCall(LValue lvalue) {
		FunctionCall funcCall = new FunctionCall();
		funcCall.lvalue = lvalue;
		
		expect(TokenType.L_PARA);
		
		// empty argument list
		if(accept(TokenType.R_PARA)) return funcCall;
		
		// at least one argument, follow up arguments separated
		// by comma.
		funcCall.arguments.add(expression());
		while(!accept(TokenType.R_PARA)) {
			if(token.type == TokenType.EOF) error("Expected '('");
			expect(TokenType.COMMA);
			funcCall.arguments.add(expression());
		}
		
		return funcCall;
	}
	
	public IfStatement ifStmt() {
		IfStatement ifStmt= new IfStatement();
		
		expect("if");
		ifStmt.condition = expression();
		expect("then");
		while(!("else".equals(token.text)) && !("end".equals(token.text))) {
			ifStmt.trueStatements.add(statement());
		}
		
		if(accept("else")) {
			while(!accept("end")) {
				if(token.type == TokenType.EOL) error("Expected 'end'");
				ifStmt.trueStatements.add(statement());
			}
		} else {
			expect("end");
		}
		return ifStmt;
	}
	
	public WhileStatement whileStmt() {
		WhileStatement whileStmt = new WhileStatement();
		
		expect("while");
		whileStmt.condition = expression();
		expect("do");
		
		while(!accept("end")) {
			if(token.type == TokenType.EOF) error("Expected 'end'");
			whileStmt.statements.add(statement());
		}
		
		return whileStmt;
	}
	
	public ReturnStatement returnStatement() {
		expect("return");
		// FIXME expression!
		return new ReturnStatement();
	}
	
	public Expression expression() {
		return comparisonExpr();
	}
	
	private Expression comparisonExpr () {
		Expression expr = logicalExpr();
		while(accept(TokenType.LESS) ||
				accept(TokenType.LESSE) ||
				accept(TokenType.EQUAL) ||
				accept(TokenType.NOTEQUAL) ||
				accept(TokenType.GREATERE) ||
				accept(TokenType.GREATER)) {
			BinaryExpression comp = new BinaryExpression();
			
			String op = lastToken.text;
			if(op.equals("<")) comp.operator = BinaryOperator.Less;
			else if(op.equals("<=")) comp.operator = BinaryOperator.LessEqual;
			else if(op.equals("==")) comp.operator = BinaryOperator.Equal;
			else if(op.equals("!=")) comp.operator = BinaryOperator.NotEqual;
			else if(op.equals(">=")) comp.operator = BinaryOperator.GreaterEqual;
			else if(op.equals(">")) comp.operator = BinaryOperator.Greater;
			else error("Unknown logical operator '" + op + "', this should never happen");
			
			comp.left = expr;
			comp.right = logicalExpr();
			expr = comp;
		}
		return expr;
	}

	private Expression logicalExpr () {
		Expression expr = additiveExpr();
		while(accept(TokenType.LAND) ||
			   accept(TokenType.LOR)) {
			BinaryExpression log = new BinaryExpression();
			log.operator = lastToken.text.equals("&&")?BinaryOperator.And: BinaryOperator.Or;
			log.left = expr;
			log.right = additiveExpr();
			expr = log;
		}
		return expr;
	}

	private Expression additiveExpr () {
		Expression expr = multiplicativeExpr();
		while(accept(TokenType.PLUS) ||
			   accept(TokenType.MINUS)) {
			BinaryExpression add = new BinaryExpression();
			add.operator = lastToken.text.equals("+")?BinaryOperator.Add: BinaryOperator.Subtract;
			add.left = expr;
			add.right = multiplicativeExpr();
			expr = add;
		}
		return expr;
	}

	private Expression multiplicativeExpr () {
		Expression expr = binaryExpr();
		while(accept(TokenType.MUL) ||
			   accept(TokenType.DIV) ||
			   accept(TokenType.MOD)) {
			BinaryExpression mul = new BinaryExpression();
			String op = lastToken.text;
			if("*".equals(op)) mul.operator = BinaryOperator.Multiply;
			else if("/".equals(op)) mul.operator = BinaryOperator.Divide;
			else if("%".equals(op)) mul.operator = BinaryOperator.Modulo;
			else error("Unexpected multiplicative operator '" + op + "', this should never happen");
			mul.left = expr;
			mul.right = binaryExpr();
			expr = mul;
		}
		return expr;
	}

	private Expression binaryExpr () {
		Expression expr = unaryExpr();
		while(accept(TokenType.SHL) ||
			   accept(TokenType.SHR) ||
			   accept(TokenType.OR) ||
			   accept(TokenType.AND) ||
			   accept(TokenType.XOR)) {
			BinaryExpression bin = new BinaryExpression();
			String op = lastToken.text;
			if("<<".equals(op)) bin.operator = BinaryOperator.ShiftLeft;
			else if(">>".equals(op)) bin.operator = BinaryOperator.ShiftRight;
			else if("|".equals(op)) bin.operator = BinaryOperator.Or;
			else if("&".equals(op)) bin.operator = BinaryOperator.And;
			else if("^".equals(op)) bin.operator = BinaryOperator.Xor;
			else error("Unexpected binary operator '" + op + "', this should never happen");
			bin.left = expr;
			bin.right = unaryExpr();
			expr = bin;
		}
		return expr;
	}

	private Expression unaryExpr () {
		if(accept(TokenType.MINUS)) {
			UnaryExpression unary = new UnaryExpression();
			unary.operator = UnaryOperator.Negate;
			unary.factor = factor();
			return unary;
		} else if(accept(TokenType.NOT)) {
			UnaryExpression unary = new UnaryExpression();
			unary.operator = UnaryOperator.Not;
			unary.factor = factor();
			return unary;
		} else {
			return factor();
		}
	}

	private Expression factor () {
		if(accept(TokenType.NUMBER)) {
			Number number = new Number();
			number.value = lastToken.text;
			return number;
		} else if(accept(TokenType.LITERAL)) {
			Literal literal = new Literal();
			literal.value = lastToken.text;
			return literal;
		} else if(accept(TokenType.L_PARA)) {
			Expression expr = expression();
			expect(TokenType.R_PARA);
			return expr;
		}else {
			int references = 0;
			while(accept(TokenType.AT)) {
				references++;
			}
			
			LValue lvalue = lvalue();
			if(token.type == TokenType.L_PARA) {
				FunctionReturnValue returnValue = new FunctionReturnValue();
				returnValue.references = references;
				returnValue.functionCall = functionCall(lvalue);
				return returnValue;
			} else {
				RValue rValue = new RValue();
				rValue.references = references;
				rValue.lvalue = lvalue;
				return rValue;
			}
		}
	}

	public static void main (String[] args) throws FileNotFoundException {
		Lexer lexer = new Lexer(new FileInputStream("data/simple.crux"), false, true);
		CruxParser parser = new CruxParser(lexer);
		parser.parse();
		Program program = parser.getProgram();
	}
}