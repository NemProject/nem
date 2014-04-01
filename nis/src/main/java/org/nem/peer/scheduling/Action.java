package org.nem.peer.scheduling;

/**
 * Interface for executing an action given an element.
 */
public interface Action<T> {

	/**
	 * Executes the action.
	 *
	 * @param element The element.
	 */
	void execute(final T element);
}