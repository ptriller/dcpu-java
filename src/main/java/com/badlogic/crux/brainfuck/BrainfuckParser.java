package com.badlogic.crux.brainfuck;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.crux.Lexer;
import com.badlogic.crux.Lexer.TokenType;
import com.badlogic.crux.Parser;
import com.badlogic.crux.brainfuck.AstNode.Instruction;
import com.badlogic.crux.brainfuck.AstNode.Loop;
import com.badlogic.crux.brainfuck.AstNode.Program;
import com.badlogic.crux.brainfuck.AstNode.Simple;
import com.badlogic.crux.brainfuck.AstNode.Simple.SimpleType;

/**
 * Brainfuck parser, using the following grammar:
 * 
 * program     ::= instruction*
 * instruction ::= loop | simple
 * loop        ::= '[' instruction* ']'
 * simple      ::= '+' | '-' | '<' | '>' | '.' | ','
 * 
 * @author mzechner
 *
 */
public class BrainfuckParser extends Parser {
	protected Program program;
	
	public BrainfuckParser (Lexer lexer) {
		super(lexer, new String[0]);
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
		do {
			program.instructions.add(instruction());
		} while(token.type != TokenType.EOF);
		return program;
	}
	
	public Instruction instruction() {
		if(token.type == TokenType.L_BRACK) {
			return loop();
		} else {
			return simple();
		}
	}
	
	public Loop loop() {
		Loop loop = new Loop();
		expect(TokenType.L_BRACK);
		do {
			loop.instructions.add(instruction());
		} while(!accept(TokenType.R_BRACK));
		return loop;
	}
	
	public Simple simple() {
		if(accept(TokenType.PLUS)) {
			return new Simple(SimpleType.IncreaseData);
		} else if(accept(TokenType.MINUS)) {
			return new Simple(SimpleType.DecreaseData);
		} else if(accept(TokenType.LESS)) {
			return new Simple(SimpleType.DecreaseDataPointer);
		} else if(accept(TokenType.GREATER)) {
			return new Simple(SimpleType.IncreaseDataPointer);
		} else if(accept(TokenType.PERIOD)) {
			return new Simple(SimpleType.OutputData);
		} else if(accept(TokenType.COMMA)) {
			return new Simple(SimpleType.InputData);
		} else {
			error("Expected +, -, <, >, ., or ,");
			return null;
		}
	}
	
	public static void main (String[] args) throws FileNotFoundException {
		Lexer lexer = new Lexer(new FileInputStream("data/simple.brainfuck"), false, false);
		BrainfuckParser parser = new BrainfuckParser(lexer);
		parser.parse();
		Program program = parser.getProgram();
//		new CEmitter().emit(System.out, program);
		new JavaEmitter().emit(System.out, program, "Test");
	}
}
