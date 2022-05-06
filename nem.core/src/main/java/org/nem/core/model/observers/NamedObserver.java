package org.nem.core.model.observers;

/**
 * Interface for observers that have a name.
 */
public interface NamedObserver {

	/**
	 * Gets the name of the observer.
	 *
	 * @return The name of the observer.
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}
}
