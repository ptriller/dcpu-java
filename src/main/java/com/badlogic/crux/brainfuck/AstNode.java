package com.badlogic.crux.brainfuck;

import java.util.ArrayList;
import java.util.List;

/**
 * Brainfuck AST nodes.
 * @author mzechner
 *
 */
public interface AstNode {
	/**
	 * program consisting of a list of instructions.
	 * @author mzechner
	 *
	 */
	public static class Program implements AstNode {
		public List<Instruction> instructions = new ArrayList<Instruction>();
	}
	
	/**
	 * Either a {@link Simple} or {@link Loop} AST node
	 * @author mzechner
	 *
	 */
	public interface Instruction extends AstNode {
	}
	
	/**
	 * One of the six operations of brainfuck.
	 * @author mzechner
	 *
	 */
	public static class Simple implements Instruction {
		public enum SimpleType {
			IncreaseDataPointer,
			DecreaseDataPointer,
			IncreaseData,
			DecreaseData,
			InputData,
			OutputData
		}
		public final SimpleType type;
		
		public Simple(SimpleType type) {
			this.type = type;
		}
	}
	
	/**
	 * Loop with nested instructions. 
	 * @author mzechner
	 */
	public static class Loop implements Instruction {
		public List<Instruction> instructions = new ArrayList<Instruction>();
	}
}
