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
	
	public static enum Register {
		A(0),
		B(1),
		C(2),
		X(3),
		Y(4),
		Z(5),
		I(6),
		J(7),
		PC(8),
		SP(9),
		O(10);
		
		public final int index;
		public final String mnemonic;

		private Register(int index) {
			this.index = index;
			this.mnemonic = this.name().toLowerCase();
		}
	}
	
	class StorageLocation {
		boolean isReg;
		int address;
		
		public void set(int val) {
			if(isReg) reg[address] = (short)val;
			else mem[address] = (short)val;
		}
		
		public int get() {
			return isReg?reg[address]: mem[address];
		}
	}
	
	
	public static final int RAM_SIZE = 0x10000;
	public static final int REGISTERS = 8 + 3;
	public static final Opcode[] OPCODES = { Opcode.NON, Opcode.SET,
			Opcode.ADD, Opcode.SUB, Opcode.MUL, Opcode.DIV, Opcode.MOD,
			Opcode.SHL, Opcode.SHR, Opcode.AND, Opcode.BOR, Opcode.XOR,
			Opcode.IFE, Opcode.IFN, Opcode.IFG, Opcode.IFB }; 
	
	private final short mem[] = new short[RAM_SIZE];
	private final short reg[] = new short[REGISTERS];
	private final StorageLocation storageLocation = new StorageLocation();
	private int cycles;
	private boolean skipNext;
	
	public Cpu(short[] mem) {
		if(mem.length > RAM_SIZE) throw new RuntimeException("mem length must be < " + RAM_SIZE);
		System.arraycopy(mem, 0, this.mem, 0, mem.length);
	}
	
	public static short[] loadDump(String dumpFile) {
		FileReader reader = null;
		try {
			reader = new FileReader(new File(dumpFile));
			StringBuffer buffer = new StringBuffer();
			int c = reader.read();
			while (c != -1) {
				buffer.append((char) c);
				c = reader.read();
			}
			String[] tokens = buffer.toString().split("\\s");
			short[] mem = new short[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				mem[i] = (short)Integer.parseInt(tokens[i], 16);
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
			return mem[mem[reg[Register.PC.index]++] + reg[b - 0x10]];			
		case 0x18:
			return mem[reg[Register.SP.index]++];
		case 0x19:
			return mem[reg[Register.SP.index]];
		case 0x1a:
			return mem[--reg[Register.SP.index]];
		case 0x1b:
			return reg[Register.SP.index];
		case 0x1c:
			return reg[Register.PC.index];
		case 0x1d:
			return reg[Register.O.index];
		case 0x1e:
			cycles++;
			return mem[mem[reg[Register.PC.index]++]];
		case 0x1f:
			cycles++;
			return mem[reg[Register.PC.index]++];
		default:
			if(b >= 0x20 && b <= 0x3f) return b - 0x20;
			throw new RuntimeException("Unkown load operator 0x" + Integer.toHexString(b));
		}		
	}
	
	private void store(int a) {
		switch(a) {
		case 0x0:
		case 0x1:
		case 0x2:
		case 0x3:
		case 0x4:
		case 0x5:
		case 0x6:
		case 0x7:
			storageLocation.isReg = true;
			storageLocation.address = a;
			return;
		case 0x8:
		case 0x9:
		case 0xa:
		case 0xb:
		case 0xc:
		case 0xd:
		case 0xe:
		case 0xf:
			storageLocation.isReg = false;
			storageLocation.address = reg[a - 0x8];
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
			storageLocation.isReg = false;
			storageLocation.address = mem[reg[Register.PC.index]++] + reg[a - 0x10];			
			return;
		case 0x18:
			storageLocation.isReg = false;
			storageLocation.address = reg[Register.SP.index]++;
			return;
		case 0x19:
			storageLocation.isReg = false;
			storageLocation.address = reg[Register.SP.index];
			return;
		case 0x1a:
			storageLocation.isReg = false;
			storageLocation.address = --reg[Register.SP.index];
			return;
		case 0x1b:
			storageLocation.isReg = true;
			storageLocation.address = Register.SP.index;
			return;			
		case 0x1c:
			storageLocation.isReg = true;
			storageLocation.address = Register.PC.index;
			return;
		case 0x1d:
			storageLocation.isReg = true;
			storageLocation.address = Register.O.index;
			return;
		case 0x1e:
			cycles++;
			storageLocation.isReg = false;
			storageLocation.address = mem[reg[Register.PC.index]++];
			return;
		case 0x1f:
			cycles++;
			return;
		default:
			// no-op
		}		
	}
	
	public void tick() {
		int v = mem[reg[Register.PC.index]++];
		Opcode opcode = OPCODES[v & 0xf];
		int a = (v & 0x3f0) >>> 4;
		int b = (v & 0xfc00) >>> 10;
		
		cycles += opcode.cycles;
		if(skipNext) {
			cycles++;
			if((b >= 0x10 && b <= 0x17) || b == 0x1e || b == 0x1f) reg[Register.PC.index]++;
			skipNext = false;
			return;
		}
		
		switch(opcode) {
		case NON:
			// JSRE
			if(a == 0x1) {
				int val = load(b);
				mem[Register.SP.index] = reg[Register.PC.index];
				reg[Register.PC.index] = (short)val;
			}
			break;
		case SET:
			store(a);
			storageLocation.set(load(b));
			break;
		case ADD:
			store(a);
			int val = storageLocation.get() + load(b);
			storageLocation.set(val);
			reg[Register.O.index] = (short)(val >>> 16);
			break;
		case SUB:
			store(a);
			val = storageLocation.get() - load(b);
			storageLocation.set(val);
			reg[Register.O.index] = (short)(val >>> 16);
			break;
		case MUL:
			store(a);
			val = storageLocation.get() * load(b);
			storageLocation.set(val);
			reg[Register.O.index] = (short)(val >>> 16);
			break;
		case DIV:
			store(a);
			val = load(b);
			if(val != 0) val = storageLocation.get() / val;
			storageLocation.set(val);			
			reg[Register.O.index] = (short)(val >>> 16);
			break;
		case MOD:
			store(a);
			val = load(b);
			if(val != 0) val = storageLocation.get() % val;
			storageLocation.set(val);			
			break;
		case SHL:
			store(a);
			val = storageLocation.get() << load(b);
			reg[Register.O.index] = (short)(val >>> 16);
			storageLocation.set(val);
			break;
		case SHR:
			store(a);
			val = storageLocation.get() >>> load(b);
			reg[Register.O.index] = (short)(val >>> 16);
			storageLocation.set(val);
			break;
		case AND:
			store(a);
			val = storageLocation.get() & load(b);
			storageLocation.set(val);
			break;
		case BOR:
			store(a);
			val = storageLocation.get() | load(b);
			storageLocation.set(val);
			break;
		case XOR:
			store(a);
			val = storageLocation.get() ^ load(b);
			storageLocation.set(val);
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
	
	public int getRegValue(Register register) {
		return reg[register.index];
	}
	
	public int getMemValue(int address) {
		return mem[address];
	}

	public int getCycles() {
		return cycles;
	}
	
	public short[] getMemory() {	
		return mem;
	}

	public boolean isNextSkipped() {
		return skipNext;
	}
}