package com.badlogic.asm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import com.badlogic.dcpu.Assembler;

public class AsmParserTest {

	@Test
	public void test() throws Exception {
		InputStream is = AsmParserTest.class.getResourceAsStream("/codeTest1.asm");
		Assembler asm = new Assembler();
		AsmInternalParser parser = new AsmInternalParser(asm, is, "UTF-8");
		parser.file();
		short[] dump = asm.getDump();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(new File("result.dcpu16")));
		for (short s : dump) {
			byte b = (byte) ((s&0xff00) >> 8);
			os.write(b);
			b = (byte) (s&0xff);
			os.write(b);
		}
		os.close();
	}

}
