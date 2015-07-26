package org.nem.core.model.primitive;

/**
 * Abstract base class for strongly typed non-negative quantities.
 *
 * @param <TDerived> The derived class type.
 */
public abstract class AbstractQuantity<
		TDerived extends AbstractQuantity<?>>
		extends AbstractPrimitive<TDerived, Long> {

	/**
	 * Creates a quantity.
	 *
	 * @param quantity The quantity.
	 * @param derivedClass The derived class.
	 */
	protected AbstractQuantity(final Long quantity, final Class<TDerived> derivedClass) {
		super(quantity, derivedClass);

		if (quantity < 0) {
			throw new NegativeQuantityException(quantity);
		}
	}

	/**
	 * Wraps a raw quantity in a strongly typed quantity
	 *
	 * @param quantity The raw quantity.
	 * @return The strongly typed quantity.
	 */
	protected abstract TDerived createFromRaw(final long quantity);

	/**
	 * Creates a new quantity by adding the specified quantity to this quantity.
	 *
	 * @param quantity The specified quantity.
	 * @return The new quantity.
	 */
	public TDerived add(final TDerived quantity) {
		return this.createFromRaw(this.getRaw() + quantity.getRaw());
	}

	/**
	 * Creates a new quantity by subtracting the specified quantity from this quantity.
	 *
	 * @param quantity The specified quantity.
	 * @return The new quantity.
	 */
	public TDerived subtract(final TDerived quantity) {
		return this.createFromRaw(this.getRaw() - quantity.getRaw());
	}

	/**
	 * Returns the quantity.
	 *
	 * @return The quantity.
	 */
	public long getRaw() {
		return this.getValue();
	}
}
