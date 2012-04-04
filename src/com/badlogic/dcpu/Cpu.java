package com.badlogic.dcpu;

/**
 * Cpu emulation for <a href="http://0x10c.com/doc/dcpu-16.txt">dcpu-16</a>
 * @author mzechner
 *
 */
public class Cpu {
	public static final int NON = 0x0;
	public static final int SET = 0x1;
	public static final int ADD = 0x2;
	public static final int SUB = 0x3;
	public static final int MUL = 0x4;
	public static final int DIV = 0x5;
	public static final int MOD = 0x6;
	public static final int SHL = 0x7;
	public static final int SHR = 0x8;
	public static final int AND = 0x9;
	public static final int BOR = 0x9;
	public static final int XOR = 0xa;
	public static final int IFE = 0xb;
	public static final int IFN = 0xc;
	public static final int IFG = 0xd;
	public static final int IFB = 0xf;
	
	
	public static final int RAM_SIZE = 0x10000;
	public static final int REGISTERS = 8;
	
	private final int mem[] = new int[RAM_SIZE];
	private final int reg[] = new int[REGISTERS];
	private int pc;
	private int sp;
	private int o;
	private int cycles;
	
	public Cpu(int[] mem) {
		if(mem.length > RAM_SIZE) throw new RuntimeException("mem length must be < " + RAM_SIZE);
		System.arraycopy(mem, 0, this.mem, 0, mem.length);
	}
	
	public void tick() {
		int v = mem[pc];
		int opcode = v & 0xF;
		int a = v & 0x3f0;
		int b = v & 0xfc00;
		
		
	}
}
