package org.nem.nis.mappers;

/**
 * A mapping type pair that describes the source and target types of a mapping.
 */
public class MappingTypePair {
	private final Class<?> sourceClass;
	private final Class<?> targetClass;

	/**
	 * Creates a new mapping type pair.
	 *
	 * @param sourceClass The source mapping class.
	 * @param targetClass The target mapping class.
	 */
	public MappingTypePair(final Class<?> sourceClass, final Class<?> targetClass) {
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	/**
	 * Gets the source class.
	 *
	 * @return The source class.
	 */
	public Class<?> getSourceClass() {
		return this.sourceClass;
	}

	/**
	 * Gets the target class.
	 *
	 * @return The target class.
	 */
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	@Override
	public int hashCode() {
		return this.sourceClass.hashCode() ^ this.targetClass.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MappingTypePair)) {
			return false;
		}

		final MappingTypePair rhs = (MappingTypePair) obj;
		return this.sourceClass.equals(rhs.sourceClass) && this.targetClass.equals(rhs.targetClass);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.sourceClass.getName(), this.targetClass.getName());
	}
}
