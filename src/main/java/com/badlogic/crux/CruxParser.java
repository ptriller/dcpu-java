package com.badlogic.crux;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.crux.AstNode.Assignment;
import com.badlogic.crux.AstNode.FunctionCall;
import com.badlogic.crux.AstNode.FunctionDefinition;
import com.badlogic.crux.AstNode.IfStatement;
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
			"var", "num", "struct", "func", "end", "if", "then", "else", "while", "return"
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
			if(accept("var")) {
				funcDef.localVars.add(varDef());
			} else {
				funcDef.statements.add(statement());
			}
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
		}
		
		return null;
	}
	
	public Assignment assignment() {
		Assignment assignment = new Assignment();
		return assignment;
	}
	
	public FunctionCall functionCall() {
		FunctionCall funcCall = new FunctionCall();
		
		return funcCall;
	}
	
	public IfStatement ifStmt() {
		IfStatement ifStmt= new IfStatement();
		
		return ifStmt;
	}
	
	public WhileStatement whileStmt() {
		WhileStatement whileStmt = new WhileStatement();
		
		return whileStmt;
	}
	
	public ReturnStatement returnStatement() {
		expect("return");
		return new ReturnStatement();
	}
	
	public static void main (String[] args) throws FileNotFoundException {
		Lexer lexer = new Lexer(new FileInputStream("data/simple.crux"), false, true);
		CruxParser parser = new CruxParser(lexer);
		parser.parse();
		Program program = parser.getProgram();
	}
}