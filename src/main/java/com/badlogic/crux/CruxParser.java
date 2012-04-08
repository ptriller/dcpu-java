package com.badlogic.crux;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.crux.AstNode.Assignment;
import com.badlogic.crux.AstNode.Dereference;
import com.badlogic.crux.AstNode.Expression;
import com.badlogic.crux.AstNode.FunctionCall;
import com.badlogic.crux.AstNode.FunctionDefinition;
import com.badlogic.crux.AstNode.IfStatement;
import com.badlogic.crux.AstNode.LValue;
import com.badlogic.crux.AstNode.OffsetDereference;
import com.badlogic.crux.AstNode.Program;
import com.badlogic.crux.AstNode.ReturnStatement;
import com.badlogic.crux.AstNode.ReturnType;
import com.badlogic.crux.AstNode.Statement;
import com.badlogic.crux.AstNode.StructureDeclaration;
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
			"var", "num", "struct", "func", "end", "if", "then", "else", "while", "do", "return"
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
			varDecl.pointerIndirections++;
		}
			
		if(accept("num")) {
			expect(TokenType.IDENTIFIER);
			varDecl.type = "num";
			varDecl.identifier = lastToken.text;
		} else if(accept(TokenType.IDENTIFIER)) {
			varDecl.type = lastToken.text;
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
	
	public FunctionDefinition funcDef() {
		FunctionDefinition funcDef = new FunctionDefinition();
		
		expect("func");
		expect(TokenType.IDENTIFIER);
		funcDef.identifier = lastToken.text;
		
		// argument list
		if(accept(TokenType.L_PARA)) {
			funcDef.arguments.add(varDef());
			while(accept(TokenType.COMMA)) {
				funcDef.arguments.add(varDef());
			}
			expect(TokenType.R_PARA); 
		}
		
		// return type
		if(accept(TokenType.COLON)) {
			funcDef.returnType = returnType();
		}
		
		// local vars and statements
		while(!accept("end")) {
			if(token.type == TokenType.EOF) error("expected end");
			funcDef.statements.add(statement());
		}
		return funcDef;
	}
	
	public ReturnType returnType() {
		ReturnType returnType = new ReturnType();
		
		while(accept(TokenType.AT)) {
			returnType.pointerIndirections++;
		}
			
		if(accept("num")) {
			returnType.type = "num";
		} else if(accept(TokenType.IDENTIFIER)) {
			returnType.type = lastToken.text;
		} else {
			error("Expected 'num' or struct name");
		}
		
		return returnType;
	}
	
	public Statement statement() {
		if("return".equals(token.text)) {
			return returnStatement();
		} else if("if".equals(token.text)) {
			return ifStmt();
		} else if("while".equals(token.text)) {
			return whileStmt();
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
			error("Expected [dereference] or identifier");
		}
		if(accept(TokenType.PERIOD)) {
			lvalue.indirection = lvalue();
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
		Expression expression = new Expression();
		return expression;
	}
	
	public static void main (String[] args) throws FileNotFoundException {
		Lexer lexer = new Lexer(new FileInputStream("data/simple.crux"), false, true);
		CruxParser parser = new CruxParser(lexer);
		parser.parse();
		Program program = parser.getProgram();
	}
}