package com.badlogic.dcpu;

public class ShortArray {
	short[] elements = new short[16];
	int size;
	
	private void ensureCapacity(int numElements) {
		int required = size + numElements;
		if(required < elements.length) return;
		short[] tmp = new short[required];
		System.arraycopy(elements, 0, tmp, 0, elements.length);
		elements = tmp;
	}
	
	public void add(short value) {
		ensureCapacity(1);
		elements[size++] = value;
	}
	
	public short get(int index) {
		return elements[index];
	}
	
	public void set(int index, short value) {
		elements[index] = value;
	}
	
	public int size() {
		return size;
	}
}
