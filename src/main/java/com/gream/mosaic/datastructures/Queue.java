package com.gream.mosaic.datastructures;

import java.util.*;

public class Queue<T> implements Iterable<T> {

	private LinkedList<T> elements = new LinkedList<T>();

	public void enqueue(T element) {
		elements.add(element);
	}

	public T dequeue() {
		if (size() == 0)
			return null;
		return elements.removeFirst();
	}

	public T peek() {
		return elements.getFirst();
	}

	public void clear() {
		elements.clear();
	}

	public int size() {
		return elements.size();
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public Iterator<T> iterator() {
		return elements.iterator();
	}
}