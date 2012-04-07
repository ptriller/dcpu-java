package com.badlogic.crux;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.crux.Lexer.Token;
import com.badlogic.crux.Lexer.TokenType;

/**
 *	Base class for various LL(1) language grammar parsers.
 * 
 * @author mzechner
 *
 */
public abstract class Parser {
	final Lexer lexer;
	Token lastToken;
	Token token;
	Token lookAhead;
	Set<String> keywords = new HashSet<String>();
	
	public Parser(Lexer lexer, String[] keywords) {
		this.lexer = lexer;
		for(String keyword: keywords) {
			this.keywords.add(keyword);
		}
		token = lexer.nextToken();
		if(token.type == TokenType.EOF) lookAhead = token;
		else lookAhead = lexer.nextToken();
	}
	
	public abstract void parse();
	
	protected void nextToken() {
		lastToken = token;
		token = lookAhead;
		if(token.type == TokenType.ERROR) error(token.text);
		if(token.type == TokenType.EOF) lookAhead = token;
		else lookAhead = lexer.nextToken();
	}
	
	protected boolean accept(TokenType type) {
		if(token.type == type) {
			nextToken();
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean accept(String keyword) {
		if(!keywords.contains(keyword)) {
			error("'" + keyword + "' is not a keyword, internal parser error");
			return false;
		}
		if(token.type == TokenType.IDENTIFIER && keyword.equals(token.text)) {
			nextToken();
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean expect(TokenType type) {
		if(accept(type)) {
			return true;
		}
		error("Expected '" + type + "'");
		return false;
	}
	
	protected boolean expect(String keyword) {
		if(accept(keyword)) return true;
		error("Expected '" + keyword + "'");
		return false;
	}
	
	protected boolean lookAhead(String keyword) {
		boolean result = keyword.equals(lookAhead.text);
		if(result) nextToken();
		return result;
	}
	
	protected boolean lookAhead(TokenType type) {
		boolean result = type == lookAhead.type;
		if(result) nextToken();
		return result;
	}
	
	protected void error(String message) {
		throw new RuntimeException("error [" +  token.line + ":" + token.col + "]: " + message + ", " + token);
	}
}
