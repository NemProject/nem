package org.nem.core.utils;

import java.util.*;

/**
 * Circular stack is a last in first out buffer with fixed size that replace its oldest element if full.
 *
 * The removal order is inverse of insertion order. The iteration order is the same as insertion order.
 *
 * <strong>Note that implementation is not synchronized.</strong>
 *
 * @param <E> Type of elements on the stack.
 */
public class CircularStack<E> implements Iterable<E> {
	private final List<E> elements = new LinkedList<>();
	private final int limit;

	/**
	 * Creates circular stack with at most limit elments.
	 *
	 * @param limit Maximum number of elements on the stack.
	 */
	CircularStack(final int limit) {
		this.limit = limit;
	}

	/**
	 * Adds element to the stack.
	 *
	 * @param element Element to be added.
	 */
	public void add(E element) {
		elements.add(element);
		if (elements.size() > limit) {
			elements.remove(0);
		}
	}

	/**
	 * Gets most recently added element.
	 *
	 * @return Most recently added element.
	 */
	public E get() {
		return elements.get(elements.size() - 1);
	}

	/**
	 * Removes most recently added element.
	 */
	public void remove() {
		elements.remove(elements.size() - 1);
	}

	/**
	 * Returns size of a stack.
	 *
	 * @return Size of a stack (can be smaller than limit).
	 */
	public int size() {
		return elements.size();
	}


	@Override
	public Iterator<E> iterator() {
		return this.elements.iterator();
	}
}
