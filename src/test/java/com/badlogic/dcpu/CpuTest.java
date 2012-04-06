package com.badlogic.dcpu;

import org.junit.Test;
import static junit.framework.TestCase.*;

import com.badlogic.dcpu.Assembler.Arg;
import com.badlogic.dcpu.Cpu.Opcode;
import com.badlogic.dcpu.Cpu.Register;

public class CpuTest {
	private Cpu run(Assembler asm) {
		short[] dump = asm.getDump();
		System.out.println(Disassembler.disassemble(dump, 0, dump.length));
		Cpu cpu = new Cpu(dump);
		cpu.runUntilHalted();
		return cpu;
	}
	
	@Test
	public void testJsr() {
		Assembler asm = new Assembler();
		asm.eop(Opcode.JSR, asm.label("main"));
		
		asm.markLabel("func");
		asm.op(Opcode.SHR, Arg.reg(Register.X), Arg.lit(0x4));
		asm.op(Opcode.SET, Arg.pc(), Arg.pop());
		
		asm.markLabel("main");
		asm.op(Opcode.SET, Arg.reg(Register.X), Arg.lit(0xf0));
		asm.eop(Opcode.JSR, asm.label("func"));
		Cpu cpu = run(asm);
		assertEquals(0xf, cpu.getRegValue(Register.X));
	}
	
	@Test
	public void testOverflow() {
		Assembler asm = new Assembler();
		asm.op(Opcode.SET, Arg.reg(Register.A), Arg.lit(0xffff));
		asm.op(Opcode.ADD, Arg.reg(Register.A), Arg.lit(0x2));
		Cpu cpu = run(asm);
		assertEquals((short)(0xfffff + 0x2), cpu.getRegValue(Register.A));
		
		asm = new Assembler();
		asm.op(Opcode.SET, Arg.reg(Register.A), Arg.lit(0x0));
		asm.op(Opcode.SUB, Arg.reg(Register.A), Arg.lit(0x2));
		cpu = run(asm);
		assertEquals((short)(0x0 - 0x2), cpu.getRegValue(Register.A));
	}
}
