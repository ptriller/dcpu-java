package com.badlogic.crux.brainfuck;

import java.io.PrintStream;

import com.badlogic.crux.brainfuck.AstNode.Instruction;
import com.badlogic.crux.brainfuck.AstNode.Loop;
import com.badlogic.crux.brainfuck.AstNode.Program;
import com.badlogic.crux.brainfuck.AstNode.Simple;

/**
 * Takes a {@link Program} and outputs C code.
 * @author mzechner
 *
 */
public class CEmitter {
	PrintStream out;
	int depth;
	
	public void emit(PrintStream out, Program program) {
		this.out = out;
		this.depth = 0;
		emit("#include <stdio.h>");
		emit("");
		emit("int main(int argc, char** argv) {");
		emit("   char array[30000];");
		emit("   char *ptr=array;");
		for(Instruction inst: program.instructions) {
			emitNode(inst);
		}
		emit("   return 0;");
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
			case IncreaseData: emit("++*ptr;"); break;
			case DecreaseData: emit("--*ptr;"); break;
			case IncreaseDataPointer: emit("++ptr;"); break;
			case DecreaseDataPointer: emit("--ptr;"); break;
			case InputData: emit("*ptr=getchar();"); break;
			case OutputData: emit("putchar(*ptr);"); break;
			}
		}
		if(node instanceof Loop) {
			emit("while(*ptr) {");
			for(Instruction inst: ((Loop)node).instructions) {
				emitNode(inst);
			}
			emit("}");
		}
		depth--;
	}
}
