package com.badlogic.dcpu;

public class IntArray {
	int[] elements = new int[16];
	int size;
	
	private void ensureCapacity(int numElements) {
		int required = size + numElements;
		if(required < elements.length) return;
		int[] tmp = new int[required];
		System.arraycopy(elements, 0, tmp, 0, required);
		elements = tmp;
	}
	
	public void add(int value) {
		ensureCapacity(1);
		elements[size++] = value;
	}
	
	public int get(int index) {
		return elements[index];
	}
	
	public void set(int index, int value) {
		elements[index] = value;
	}
	
	public int size() {
		return size;
	}
}

