package com.badlogic.dcpu;

import java.io.File;
import java.io.FileReader;

/**
 * Cpu emulation for <a href="http://0x10c.com/doc/dcpu-16.txt">dcpu-16</a>
 * @author mzechner
 *
 */
public class Cpu {
	public static enum Opcode {
		NON(0x0, 0),
		SET(0x1, 1),
		ADD(0x2, 2),
		SUB(0x3, 2),
		MUL(0x4, 2),
		DIV(0x5, 3),
		MOD(0x6, 3),
		SHL(0x7, 2),
		SHR(0x8, 2),
		AND(0x9, 1),
		BOR(0xa, 1),
		XOR(0xb, 1),
		IFE(0xc, 2),
		IFN(0xd, 2),
		IFG(0xe, 2),
		IFB(0xf, 2);
	 
		public final int code;
		public final String mnemonic;
		public final int cycles;

		private Opcode(int code, int cycles) {
			this.code = code;
			this.cycles = cycles;
			this.mnemonic = this.name().toLowerCase();
		}
	}
	
	
	public static final int RAM_SIZE = 0x10000;
	public static final int REGISTERS = 8;
	public static final Opcode[] OPCODES = { Opcode.NON, Opcode.SET,
			Opcode.ADD, Opcode.SUB, Opcode.MUL, Opcode.DIV, Opcode.MOD,
			Opcode.SHL, Opcode.SHR, Opcode.AND, Opcode.BOR, Opcode.XOR,
			Opcode.IFE, Opcode.IFN, Opcode.IFG, Opcode.IFB }; 
	
	private final int mem[] = new int[RAM_SIZE];
	private final int reg[] = new int[REGISTERS];
	private int pc;
	private int sp = 0xffff;
	private int o;
	private int cycles;
	private boolean skipNext;
	
	public Cpu(int[] mem) {
		if(mem.length > RAM_SIZE) throw new RuntimeException("mem length must be < " + RAM_SIZE);
		System.arraycopy(mem, 0, this.mem, 0, mem.length);
	}
	
	public static int[] loadDump(String dumpFile) {
		FileReader reader = null;
		try {
			// yeah, let's ignore the encoding :)
			reader = new FileReader(new File(dumpFile));
			StringBuffer buffer = new StringBuffer();
			int c = reader.read();
			while (c != -1) {
				buffer.append((char) c);
				c = reader.read();
			}
			String[] tokens = buffer.toString().split("\\s");
			int[] mem = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				mem[i] = Integer.parseInt(tokens[i], 16);
			}
			return mem;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't load dump from file '"
					+ dumpFile + "'", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (Exception e) {
				}
		}
	}

	private int load(int b) {		
		switch(b) {
		case 0x0:
		case 0x1:
		case 0x2:
		case 0x3:
		case 0x4:
		case 0x5:
		case 0x6:
		case 0x7:
			return reg[b];			
		case 0x8:
		case 0x9:
		case 0xa:
		case 0xb:
		case 0xc:
		case 0xd:
		case 0xe:
		case 0xf:
			return mem[reg[b - 0x8]];
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x17:
			cycles++;
			return mem[mem[pc++] + reg[b - 0x10]];			
		case 0x18:
			return mem[sp++];
		case 0x19:
			return mem[sp];
		case 0x1a:
			return mem[--sp];
		case 0x1b:
			return sp;
		case 0x1c:
			return pc;
		case 0x1d:
			return o;
		case 0x1e:
			cycles++;
			return mem[mem[pc++]];
		case 0x1f:
			cycles++;
			return mem[pc++];
		default:
			if(b >= 0x20 && b <= 0x3f) return b - 0x20;
			throw new RuntimeException("Unkown load operator 0x" + Integer.toHexString(b));
		}		
	}
	
	private void store(int a, int val) {
		switch(a) {
		case 0x0:
		case 0x1:
		case 0x2:
		case 0x3:
		case 0x4:
		case 0x5:
		case 0x6:
		case 0x7:
			reg[a] = val;
			return;
		case 0x8:
		case 0x9:
		case 0xa:
		case 0xb:
		case 0xc:
		case 0xd:
		case 0xe:
		case 0xf:
			mem[reg[a - 0x8]] = val;
			return;
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x17:
			cycles++;
			mem[mem[pc++] + reg[a - 0x10]] = val;
			return;
		case 0x18:
			mem[sp++] = val;
			return;
		case 0x19:
			mem[sp] = val;
			return;
		case 0x1a:
			mem[--sp] = val;
			return;
		case 0x1b:
			sp = val;
			return;			
		case 0x1c:
			pc = val;
			return;
		case 0x1d:
			o = val;
			return;
		case 0x1e:
			cycles++;
			mem[mem[pc++]] = val;
			return;
		case 0x1f:
			cycles++;
			return;
		default:
			// no-op
		}		
	}
	
	public void tick() {
		int v = mem[pc++];
		Opcode opcode = OPCODES[v & 0xf];
		int a = (v & 0x3f0) >>> 4;
		int b = (v & 0xfc00) >>> 10;
		
		cycles += opcode.cycles;
		if(skipNext) {
			cycles++;
			if((b >= 0x10 && b <= 0x17) || b == 0x1e || b == 0x1f) pc++;
			pc++;
			skipNext = false;
		}
		
		switch(opcode) {
		case NON:
			break;
		case SET:
			store(a, load(b));
			break;
		case ADD:
			int sum = load(a) + load(b);
			store(a, sum);
			break;
		case SUB:
			sum = load(a) - load(b);
			store(a, sum);
			break;
		case MUL:
			int prod = load(a) * load(b);
			store(a, prod);
			break;
		case DIV:
			prod = load(a) / load(b);
			store(a, prod);
			break;
		case MOD:
			prod = load(a) % load(b);
			store(a, prod);
			break;
		case SHL:
			store(a, load(a) << load(b));
			break;
		case SHR:
			store(a, load(a) >>> load(b));
			break;
		case AND:
			store(a, load(a) & load(b));
			break;
		case BOR:
			store(a, load(a) | load(b));
			break;
		case XOR:
			store(a, load(a) ^ load(b));
			break;
		case IFE:
			if(!(load(a) == load(b))) skipNext = true;
			break;
		case IFN:
			if(!(load(a) != load(b))) skipNext = true;
			break;
		case IFG:
			if(!(load(a) > load(b))) skipNext = true;
			break;
		case IFB:
			if((load(a) & load(b)) != 0) skipNext = true;
			break;
		default:
			throw new RuntimeException("Unkown opcode 0x" + Integer.toHexString(v & 0xf));
		}
	}
	
	public static void main(String[] args) {
		Cpu cpu = new Cpu(Cpu.loadDump("data/simple.dcpu"));
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
		cpu.tick();
	}
}