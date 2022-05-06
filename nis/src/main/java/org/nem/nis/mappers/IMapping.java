package org.nem.nis.mappers;

/**
 * Interface for mapping from a source type to a target type.
 *
 * @param <TSource> The source type.
 * @param <TTarget> The target type.
 */
@FunctionalInterface
public interface IMapping<TSource, TTarget> {

	/**
	 * Maps source to the desired target type.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	TTarget map(final TSource source);
}
