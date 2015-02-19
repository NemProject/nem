package org.nem.core.model.primitive;

/**
 * Abstract base class for strongly typed primitives.
 *
 * @param <TDerived> The derived class type.
 * @param <TValue> The value type.
 */
public abstract class AbstractPrimitive<
		TDerived extends AbstractPrimitive<?, TValue>,
		TValue extends Number & Comparable<TValue>>
		implements Comparable<TDerived> {

	private final TValue value;
	private final Class<TDerived> derivedClass;

	/**
	 * Creates a new primitive.
	 *
	 * @param value The primitive value.
	 * @param derivedClass The derived class.
	 */
	protected AbstractPrimitive(final TValue value, final Class<TDerived> derivedClass) {
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
		return this.value.compareTo(rhs.getValue());
	}

	/**
	 * Returns the underlying value.
	 *
	 * @return The underlying value.
	 */
	protected TValue getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		final long longValue = this.value.longValue();
		return (int)(longValue ^ (longValue >> 32));
	}

	@Override
	public boolean equals(final Object obj) {
		if (!this.derivedClass.isInstance(obj)) {
			return false;
		}

		final TDerived rhs = this.derivedClass.cast(obj);
		return 0 == this.compareTo(rhs);
	}

	@Override
	public String toString() {
		return this.value.toString();
	}
}
