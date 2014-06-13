package org.nem.core.model.primitive;

public abstract class AdvancedAbstractPrimitive<TDerived extends AdvancedAbstractPrimitive, TValue extends Number & Comparable<TValue>> implements Comparable<TDerived> {

	private final TValue value;
	private final Class<TDerived> derivedClass;


	/**
	 * Creates a new primitive.
	 *
	 * @param value The primitive value.
	 */
	protected AdvancedAbstractPrimitive(TValue value, final Class<TDerived> derivedClass) {
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
		return this.value.compareTo((TValue)rhs.getValue());
	}

	/**
	 * Returns the underlying value.
	 *
	 * @return The underlying value.
	 */
	protected TValue getValue() { return this.value; }

	@Override
	public int hashCode() {
		return (int)(this.value.longValue() ^ (this.value.longValue() >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (!this.derivedClass.isInstance(obj))
			return false;

		final TDerived rhs = this.derivedClass.cast(obj);
		return this.value.compareTo((TValue)rhs.getValue()) == 0;
	}

	@Override
	public String toString() {
		return this.value.toString();
	}
}
