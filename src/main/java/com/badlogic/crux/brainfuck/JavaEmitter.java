package com.badlogic.crux.brainfuck;

import java.io.PrintStream;

import com.badlogic.crux.brainfuck.AstNode.Instruction;
import com.badlogic.crux.brainfuck.AstNode.Loop;
import com.badlogic.crux.brainfuck.AstNode.Program;
import com.badlogic.crux.brainfuck.AstNode.Simple;

/**
 * Takes a {@link Program} and outputs Java code.
 * @author mzechner
 *
 */
public class JavaEmitter {
	PrintStream out;
	int depth;
	
	public void emit(PrintStream out, Program program, String className) {
		this.out = out;
		this.depth = 0;
		emit("import java.io.*;");
		emit("");
		emit("public class " + className + " {");
		emit("   public static void main(String[] argv) {");
		emit("      char[] array = new char[30000];");
		emit("      int ptr=0;");
		depth = 1;
		for(Instruction inst: program.instructions) {
			emitNode(inst);
		}		
		depth = 0;
		emit("   }");
		emit("}");
	}
	
	private void emit(String message) {
		for(int i = 0; i < depth; i++) out.print("   ");
		out.println(message);
	}
	
	private void emitNode(AstNode node) {
		depth++;
		if(node instanceof Simple) {
			switch(((Simple)node).type) {
			case IncreaseData: emit("++array[ptr];"); break;
			case DecreaseData: emit("--array[ptr];"); break;
			case IncreaseDataPointer: emit("++ptr;"); break;
			case DecreaseDataPointer: emit("--ptr;"); break;
			case InputData: emit("array[ptr]=System.in.read();"); break;
			case OutputData: emit("System.out.print(array[ptr]);"); break;
			}
		}
		if(node instanceof Loop) {
			emit("while(array[ptr] != 0) {");
			for(Instruction inst: ((Loop)node).instructions) {
				emitNode(inst);
			}
			emit("}");
		}
		depth--;
	}
}

