package com.badlogic.dcpu;

import java.io.File;
import java.io.FileReader;

import com.badlogic.dcpu.Cpu.Opcode;

/**
 * Disassembler for <a href="http://0x10c.com/doc/dcpu-16.txt">dcpu-16</a>, some
 * assumptions have to be made since 
 * @author mzechner
 *
 */
public class Disassembler {
	static final String[] registerNames = { "a", "b", "c", "x", "y", "z", "i", "j" };
	
	private static int decodeArgument(int arg, int nextWord, StringBuffer buffer) {		
		switch(arg) {
		case 0x0:
		case 0x1:
		case 0x2:
		case 0x3:
		case 0x4:
		case 0x5:
		case 0x6:
		case 0x7:
			buffer.append(registerNames[arg]);
			return 0;
		case 0x8:
		case 0x9:
		case 0xa:
		case 0xb:
		case 0xc:
		case 0xd:
		case 0xe:
		case 0xf:
			buffer.append("["); 
			buffer.append(registerNames[arg - 0x8]); 
			buffer.append("]");
			return 0;
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x17:
			buffer.append("[0x"); 
			buffer.append(Integer.toHexString(nextWord & 0xffff));
			buffer.append("+");
			buffer.append(registerNames[arg - 0x10]);
			buffer.append("]");
			return 1;
		case 0x18:
			buffer.append("pop");
			return 0;
		case 0x19:
			buffer.append("peek");
			return 0;
		case 0x1a:
			buffer.append("push");
			return 0;
		case 0x1b:
			buffer.append("sp");
			return 0;
		case 0x1c:
			buffer.append("pc");
			return 0;
		case 0x1d:
			buffer.append("0");
			return 0;
		case 0x1e:
			buffer.append("[0x");
			buffer.append(Integer.toHexString(nextWord & 0xffff));
			buffer.append("]");
			return 1;
		case 0x1f:
			buffer.append("0x");
			buffer.append(Integer.toHexString(nextWord & 0xffff));
			return 1;
		default:
			if(arg >= 0x20 && arg <= 0x3f) {
				buffer.append("0x");
				buffer.append(Integer.toHexString(arg - 0x20));
				return 0;
			}
			throw new RuntimeException("Unkown load operator 0x" + Integer.toHexString(arg));
		}		
	}
		
	private static String pad(String hex) {
		if(hex.length() == 4) return hex;
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < 4 - hex.length(); i++) buffer.append("0");
		buffer.append(hex);
		return buffer.toString();
	}
	
	public static String disassemble(short[] mem, int offset, int len) {
		StringBuffer buffer = new StringBuffer();
		
		boolean lastWasJump = false;
		int end = offset + len;
		for(int pc = offset; pc < end;) {
			int v = mem[pc++];
			int oc = v & 0xf;
			if(oc > Cpu.OPCODES.length) throw new RuntimeException("Unkown opcode 0x" + Integer.toHexString(oc) + " at address " + (pc - 1));
			Opcode opcode = Cpu.OPCODES[oc];
			int a = (v & 0x3f0) >>> 4;
			int b = (v & 0xfc00) >>> 10;
			buffer.append(pad(Integer.toHexString(pc - 1)));
			buffer.append(":     ");

			if(opcode != Opcode.EXTENDED) {
				if(lastWasJump) buffer.append("   ");
				lastWasJump = opcode.mnemonic.charAt(0) == 'i';
				buffer.append(opcode.mnemonic);
				buffer.append(" ");
				pc += decodeArgument(a, pc < end? mem[pc]: 0, buffer);
				buffer.append(", ");
				pc += decodeArgument(b, pc < end? mem[pc]: 0, buffer);
	 			buffer.append("\n");
			} else {
				if(a == Opcode.JSR.extended) {
					buffer.append("jsr ");
					pc += decodeArgument(b, pc < end? mem[pc]: 0, buffer);
				} else if(a == 0) {
					buffer.append("halt");
				} else {
					buffer.append(pad(Integer.toHexString((short)v)));
					buffer.append(" (unkown extended opcode)");
				}
				buffer.append("\n");	
			}
		}
		
		return buffer.toString();
	}
	
	public static String disassembleInstr(short[] mem, int offset) {
		StringBuffer buffer = new StringBuffer();
		int pc = offset;
		int v = mem[pc++];
		int oc = v & 0xf;
		if(oc > Cpu.OPCODES.length) throw new RuntimeException("Unkown opcode 0x" + Integer.toHexString(oc) + " at address " + (pc - 1));
		Opcode opcode = Cpu.OPCODES[oc];
		int a = (v & 0x3f0) >>> 4;
		int b = (v & 0xfc00) >>> 10;
		buffer.append(pad(Integer.toHexString(pc - 1)));
		buffer.append(":     ");

		if(opcode != Opcode.EXTENDED) {
			buffer.append(opcode.mnemonic);
			buffer.append(" ");
			pc += decodeArgument(a, pc < mem.length? mem[pc]: 0, buffer);
			buffer.append(", ");
			pc += decodeArgument(b, pc < mem.length? mem[pc]: 0, buffer);
		} else {
			if(a == Opcode.JSR.extended) {
				buffer.append("jsr ");
				pc += decodeArgument(b, pc < mem.length? mem[pc]: 0, buffer);
			} else if(a == 0) {
				buffer.append("halt");
			} else {
				buffer.append(pad(Integer.toHexString((short)v)));
				buffer.append(" (unkown extended opcode)");
			}
			buffer.append("\n");	
		}
		return buffer.toString();
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
	
	public static void main(String[] args) {
		short[] dump = Disassembler.loadDump("data/simple.dcpu");
		System.out.println(Disassembler.disassemble(dump, 0, dump.length)); 
	}
}