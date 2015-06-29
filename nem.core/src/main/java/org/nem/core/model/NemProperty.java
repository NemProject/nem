package org.nem.core.model;

/**
 * Represents an entry in the nem properties.
 */
public class NemProperty {
	private final String name;
	private final String value;

	/**
	 * Creates a new nem properties entry.
	 *
	 * @param name The name.
	 * @param value The value.
	 */
	public NemProperty(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Gets the name.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.name;
	}


	/**
	 * Gets the value.
	 *
	 * @return The value.
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() ^ this.value.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NemProperty)) {
			return false;
		}

		final NemProperty rhs = (NemProperty)obj;
		return this.name.equals(rhs.name) && this.value.equals(rhs.value);
	}
}
