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
	// TODO 20140909 J-G: why did you choose a LinkedList instead of an array / ArrayList with a pointer to the current element?
	// G-J: there's no random access, so I thought linked list would be better
	// TODO 20140909 J-G: how many elements do you expect this to contain?
	// actually this class doesn't make much sense, as I've thought there might be >2 elements,
	// but later I've noticed that in most cases there will be only two elements.
	// But I'll probably use it also in BlockChain where it'll have 60 elements.
	// TODO 20140915 J-G: with that small number of elements, i would probably not use a LinkedList :)
	private final List<E> elements = new LinkedList<>();
	private final int limit;

	/**
	 * Creates circular stack with at most limit elements.
	 *
	 * @param limit Maximum number of elements on the stack.
	 */
	public CircularStack(final int limit) {
		this.limit = limit;
	}

	/**
	 * Creates shallow copy in destination.
	 *
	 * @param destination CircularStack to which elements should be copied.
	 */
	public void shallowCopyTo(final CircularStack<E> destination) {
		destination.elements.clear();
		destination.putAll(this);
	}

	private void putAll(final CircularStack<E> rhs) {
		int i = 0;
		for (final E element : rhs) {
			if (i >= rhs.size() - this.limit) {
				this.push(element);
			}
			++i;
		}
	}


	/**
	 * Adds element to the stack.
	 *
	 * @param element Element to be added.
	 */
	public void push(E element) {
		elements.add(element);
		if (elements.size() > limit) {
			elements.remove(0);
		}
	}

	// TODO 20140909 J-G: since this is a stack, i think push / pop / peek (if needed) are better names
	// G-J: not sure about pop, as I actually wouldn't like to return popped element, would it still be ok, if it would be void?
	// TODO 20140915 J-G: i think it's ok if pop doesn't return

	/**
	 * Gets most recently added element.
	 *
	 * @return Most recently added element.
	 */
	public E peek() {
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
