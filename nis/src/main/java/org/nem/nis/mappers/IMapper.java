package org.nem.nis.mappers;

/**
 * An interface for mapping a source type to a target type.
 */
public interface IMapper {

	/**
	 * Maps a source type to a target type.
	 *
	 * @param source The source object.
	 * @param targetClass The target class.
	 * @param <TSource> The source type.
	 * @param <TTarget> The target type.
	 * @return The target object.
	 */
	<TSource, TTarget> TTarget map(final TSource source, final Class<TTarget> targetClass);
}
