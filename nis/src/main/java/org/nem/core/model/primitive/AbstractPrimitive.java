package org.nem.core.model.primitive;

/**
 * Abstract base class for strongly typed primitives.
 *
 * @param <TDerived> The derived class type.
 */
public abstract class AbstractPrimitive<TDerived extends AbstractPrimitive> implements Comparable<TDerived> {

	private final long value;
	private final Class<TDerived> derivedClass;

	/**
	 * Creates a new primitive.
	 *
	 * @param value The primitive value.
	 */
	protected AbstractPrimitive(long value, final Class<TDerived> derivedClass) {
		this.value = value;
		this.derivedClass = derivedClass;
	}

	/**
	 * Compares this primitive to another TDerived instance.
	 *
	 * @param rhs The TDerived to compare against.
	 * @return -1, 0 or 1 as this TDerived is numerically less than, equal to, or greater than rhs.
	 */
	@Override
	public int compareTo(final TDerived rhs) {
		return Long.compare(this.value, rhs.getValue());
	}

	/**
	 * Returns the underlying value.
	 *
	 * @return The underlying value.
	 */
	protected long getValue() { return this.value; }

	@Override
	public int hashCode() {
		return (int)(this.value ^ (this.value >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (!this.derivedClass.isInstance(obj))
			return false;

		final TDerived rhs = this.derivedClass.cast(obj);
		return this.value == rhs.getValue();
	}

	@Override
	public String toString() {
		return String.format("%d", this.value);
	}
}
