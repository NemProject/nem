package org.nem.nis.mappers;

import java.util.*;

/**
 * A repository of mappings.
 */
public class MappingRepository implements IMapper {
	private final Map<MappingTypePair, IMapping> knownMappings = new HashMap<>();

	/**
	 * Adds a mapping to this mapper.
	 *
	 * @param sourceClass The source type.
	 * @param targetClass The target type.
	 * @param mapping The mapping
	 * @param <TSource> The source type.
	 * @param <TTarget> The target type.
	 */
	public <TSource, TTarget> void addMapping(
			final Class<TSource> sourceClass,
			final Class<? super TTarget> targetClass,
			final IMapping<TSource, TTarget> mapping) {
		final MappingTypePair pair = new MappingTypePair(sourceClass, targetClass);
		if (null != this.knownMappings.putIfAbsent(pair, mapping)) {
			throw new MappingException(String.format("cannot change mapping for pair: %s", pair));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <TSource, TTarget> TTarget map(final TSource source, final Class<TTarget> target) {
		final MappingTypePair pair = new MappingTypePair(source.getClass(), target);
		final IMapping mapping = this.knownMappings.getOrDefault(pair, null);

		if (null == mapping) {
			throw new MappingException(String.format("cannot find mapping for pair: %s", pair));
		}

		return (TTarget)mapping.map(source);
	}
}
