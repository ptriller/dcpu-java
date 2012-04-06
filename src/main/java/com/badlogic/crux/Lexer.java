
package com.badlogic.crux;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/** Lexer for Crux language.
 * @author mzechner */
public class Lexer {
	private final LookAheadReader in;
	private final boolean reportEol;

	public Lexer (InputStream in) {
		this(in, true);
	}

	public Lexer (InputStream in, boolean reportEol) {
		this.in = new LookAheadReader(in);
		this.reportEol = reportEol;
	}

	public Token nextToken () {
		int c = in.read();

		// eat whitespace
		while (c == ' ' || c == '\t') {
			c = in.read();
		}

		// eat (single line) comments
		if (c == '/' && in.lookAhead('/')) {
			while (c != '\n' && c != -1) {
				c = in.read();
			}
		}

		// EOL
		if (c == '\n' || (c == '\r' && in.lookAhead('\n'))) {
			if (reportEol)
				return new Token(TokenType.EOL, "unknown");
			else {
				c = in.read();
				while (c == '\n' || (c == '\r' && in.lookAhead('\n'))) {
					c = in.read();
				}
			}
		}

		// EOF
		if (c == -1) return new Token(TokenType.EOF, null);

		if (c == '(') return new Token(TokenType.L_PARA, "(");
		if (c == ')') return new Token(TokenType.R_PARA, ")");
		if (c == '[') return new Token(TokenType.L_BRACK, "[");
		if (c == ']') return new Token(TokenType.R_BRACK, "]");

		if (c == '+') return new Token(TokenType.PLUS, "+");
		if (c == '-') return new Token(TokenType.MINUS, "-");
		if (c == '/') return new Token(TokenType.DIV, "/");
		if (c == '*') return new Token(TokenType.MUL, "*");
		if (c == '%') return new Token(TokenType.MOD, "%");
		if (c == '<' && in.lookAhead('<')) return new Token(TokenType.SHL, "<<");
		if (c == '>' && in.lookAhead('>')) return new Token(TokenType.SHR, ">>");
		if (c == '&') return new Token(TokenType.AND, "&");
		if (c == '|') return new Token(TokenType.OR, "|");
		if (c == '^') return new Token(TokenType.XOR, "^");
		if (c == '!') {
			if (in.lookAhead('='))
				return new Token(TokenType.NOTEQUAL, "!=");
			else
				return new Token(TokenType.NOT, "!");
		}
		if (c == '>') {
			if (in.lookAhead('='))
				return new Token(TokenType.GREATERE, ">=");
			else
				return new Token(TokenType.GREATER, ">");
		}
		if (c == '<') {
			if (in.lookAhead('='))
				return new Token(TokenType.LESSE, "<=");
			else
				return new Token(TokenType.LESS, "<");
		}
		if (c == '=') {
			if (in.lookAhead('='))
				return new Token(TokenType.EQUAL, "==");
			else
				return new Token(TokenType.ASSIGN, "=");
		}

		StringBuffer text = new StringBuffer();
		
		// identifier/keyword, works like in Java, except that the first char
		// can only be '_' or a letter.
		if (c == '_' && Character.isJavaIdentifierPart(in.lookAhead)) {
			text.append((char)c);
			while (Character.isJavaIdentifierPart(in.lookAhead)) {
				text.append((char)in.read());
			}
			return new Token(TokenType.IDENTIFIER, text.toString());
		}
		if (Character.isLetter(c)) {
			text.append((char)c);
			while (Character.isJavaIdentifierPart(in.lookAhead)) {
				text.append((char)in.read());
			}
			return new Token(TokenType.IDENTIFIER, text.toString());
		}

		// hex number
		if (c == '0' && in.lookAhead('x')) {
			text.append((char)c);
			text.append('x');
			if(!(Character.isDigit(in.lookAhead) || 
					 (in.lookAhead >= 'a' && in.lookAhead <= 'f') ||
					 (in.lookAhead >= 'A' && in.lookAhead <= 'F')))
				return new Token(TokenType.ERROR, "expected '0'-'f'");
			while (Character.isDigit(in.lookAhead) || 
					 (in.lookAhead >= 'a' && in.lookAhead <= 'f') ||
					 (in.lookAhead >= 'A' && in.lookAhead <= 'F')) {
				text.append((char)in.read());
			}
			return new Token(TokenType.HEX_NUMBER, text.toString());
		}

		// bin number
		if (c == '0' && in.lookAhead('b')) {
			text.append((char)c);
			text.append('b');
			if(in.lookAhead != '0' && in.lookAhead != '1') return new Token(TokenType.ERROR, "expected '0' or '1'");
			while (in.lookAhead == '0' || in.lookAhead == '1') {
				text.append((char)in.read());
			}
			return new Token(TokenType.BIN_NUMBER, text.toString());
		}

		// number
		if(Character.isDigit(c)) {
			text.append((char)c);
			while(Character.isDigit(in.lookAhead)) {
				text.append((char)in.read());
			}
			return new Token(TokenType.NUMBER, text.toString());
		}
		
		// literal with support for \n \r \t \" and \\ to escape \
		if(c == '"') {
			c = in.read();
			while(c != '"') {
				if(c == -1) return new Token(TokenType.ERROR, "expected closing '\"'");
				if(c == '\\') {
					if(in.lookAhead('n')) {
						text.append('\n');
					} else if(in.lookAhead('r')) {
						text.append('\r');
					} else if(in.lookAhead('t')) {
						text.append('\t');
					} else if(in.lookAhead('"')) {
						text.append('"');
					} else if(in.lookAhead('\\')) {
						text.append('\\');
					} else {
						return new Token(TokenType.ERROR, "expected \\r, \\n, \\t, \\\" or \\\\");
					}
				} else {
					text.append((char)c);
				}
				c = in.read();
			}
			return new Token(TokenType.LITERAL, text.toString());
		}
		
		return new Token(TokenType.ERROR, "unexpected character '" + (char)c + "'");
	}

	enum TokenType {
		L_BRACK, // [
		R_BRACK, // ]
		L_PARA, // (
		R_PARA, // )

		PLUS, // +
		MINUS, // -
		DIV, // /
		MUL, // *
		MOD, // %
		SHL, // <<
		SHR, // >>
		AND, // &
		LAND, // &&
		OR, // |
		LOR, // ||
		NOT, // !
		XOR, // ^
		LXOR, // ^^

		GREATER, // >
		LESS, // <
		GREATERE, // >=
		LESSE, // <=
		EQUAL, // ==
		NOTEQUAL, // !=

		ASSIGN, // =
		IDENTIFIER, // (alpha)(alpha|digit|[_])*
		HEX_NUMBER, // ('0x' (digit|[abcdef])(digit|[abcdef])*) |
		BIN_NUMBER, // ('0b' [01]([01])*) |
		NUMBER, // (digit(digit)*)
		LITERAL, // ('"'('\t'|'\r'|'\n'|'\\'|[.^"])*'"'

		EOL, // ((\r\n) | (\n))
		EOF, ERROR;
	}

	public class Token {
		public final TokenType type;
		public final int line;
		public final int col;
		public final String text;

		public Token (TokenType type, String text) {
			this.type = type;
			this.line = Lexer.this.in.line();
			this.col = Lexer.this.in.col()
				- (type != TokenType.ERROR && type != TokenType.EOL && type != TokenType.EOF ? text.length() - 1 : 0);
			this.text = text;
		}

		@Override
		public String toString () {
			return "Token [type=" + type + ", line=" + line + ", col=" + col + ", text=" + text + "]";
		}
	}

	class LookAheadReader {
		Reader reader;
		int line = 1;
		int col = 1;
		int last = -2;
		int curr = -2;
		int lookAhead;

		public LookAheadReader (InputStream in) {
			try {
				reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public int read () {
			try {
				if (curr == -2) {
					curr = reader.read();
					if (curr == -1)
						lookAhead = -1;
					else
						lookAhead = reader.read();
				} else {
					if (lookAhead == -1) return -1;
					last = curr;
					curr = lookAhead;
					lookAhead = reader.read();
					if (last == '\n') {
						line++;
						col = 1;
					} else {
						col++;
					}
				}
				return curr;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public boolean lookAhead (int c) {
			boolean result = lookAhead == c;
			if (result) read();
			return result;
		}

		public int line () {
			return line;
		}

		public int col () {
			return col;
		}
	}

	public static void main (String[] args) {
		Lexer lexer = new Lexer(System.in, false);
		Token token = null;
		do {
			token = lexer.nextToken();
			System.out.println(token);
		} while (token != null && token.type != TokenType.EOF);
	}
}
