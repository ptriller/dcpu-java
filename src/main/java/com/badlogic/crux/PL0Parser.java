package com.badlogic.crux;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.badlogic.crux.Lexer.TokenType;

public class PL0Parser extends Parser {
	public PL0Parser (Lexer lexer) {
		super(lexer, new String[] {
			"CONST",
			"VAR",
			"PROCEDURE",
			"CALL",
			"BEGIN",
			"END",
			"IF",
			"THEN",
			"WHILE",
			"DO",
			"ODD",
		});
	}

	@Override
	public void parse () {
		program();
	}
	
	private void program() {
		block();
	}
	
	/**
	 *  block =
    *      ["const" ident "=" number {"," ident "=" number} ";"]
    *      ["var" ident {"," ident} ";"]
    *      {"procedure" ident ";" block ";"} statement .
	 */
	private void block() {
		if(accept("CONST")) {
			do {
				expect(TokenType.IDENTIFIER);
				expect(TokenType.ASSIGN);
				expect(TokenType.NUMBER);
			} while(accept(TokenType.COMMA));
			expect(TokenType.SEMICOLON);
		}
		if(accept("VAR")) {
			do {
				expect(TokenType.IDENTIFIER);
			} while(accept(TokenType.COMMA));
			expect(TokenType.SEMICOLON);
		}
		while(accept("PROCEDURE")) {
			expect(TokenType.IDENTIFIER);
			expect(TokenType.SEMICOLON);
			block();
			expect(TokenType.SEMICOLON);
		}
		statement();
	}
	
	/**
	 * statement =
    *     [ident ":=" expression
    *     | "call" ident
    *     | "begin" statement {";" statement} "end"
    *     | "if" condition "then" statement
    *     | "while" condition "do" statement
    *     ] .
	 */
	private void statement() {
		if(accept("CALL")) {
			expect(TokenType.IDENTIFIER);
		} else if(accept("BEGIN")) {
			do {
				statement();
			} while(accept(TokenType.SEMICOLON));
			expect("END");
		} else if(accept("IF")) {
			condition();
			expect("THEN");
			statement();
		} else if(accept("WHILE")) {
			condition();
			expect("DO");
			statement();
		} else if(accept(TokenType.IDENTIFIER)) {
			expect(TokenType.COLON);
			expect(TokenType.ASSIGN);
			expression();
		}
	}
	
	/**
	 * condition =
    *     "odd" expression
    *     | expression ("="|"#"|"<"|"<="|">"|">=") expression .
	 */
	private void condition() {
		if(accept("ODD")) {
			expression();
		} else {
			expression();
			if(token.type == TokenType.ASSIGN ||
				token.type == TokenType.HASH ||
				token.type == TokenType.LESS ||
				token.type == TokenType.LESSE ||
				token.type == TokenType.GREATER ||
				token.type == TokenType.GREATERE) {
				nextToken();
				expression();
			} else {
				error("expected comparison operator (=, #, <, <=, >, >=)");
				nextToken();
			}
		}
	}
	
	/**
	 *  expression = ["+"|"-"] term {("+"|"-") term} .
	 */
	private void expression() {
		if(token.type == TokenType.PLUS || token.type == TokenType.MINUS) {
			nextToken();
		}
		term();
		while(token.type == TokenType.PLUS || token.type == TokenType.MINUS) {
			nextToken();
			term();
		}
	}
	
	/** 
	 * term = factor {("*"|"/") factor} .
	 */
	private void term() {
		factor();
		while(token.type == TokenType.MUL || token.type == TokenType.DIV) {
			nextToken();
			factor();
		}
	}
	
	/**
	 *  factor =
    *    ident
    *    | number
    *    | "(" expression ")" .
	 */
	private void factor() {
		if(accept(TokenType.IDENTIFIER)) {
			
		} else if(accept(TokenType.NUMBER)) {
			
		} else if(accept(TokenType.L_PARA)) {
			expression();
			expect(TokenType.R_PARA);
		} else {
			error("expected identifier, number or (");
			nextToken();
		}
	}
	
	public static void main (String[] args) throws FileNotFoundException {
		Lexer lexer = new Lexer(new FileInputStream("data/simple.pl0"), false);
		Parser parser = new PL0Parser(lexer);
		parser.parse();
	}
}
