package org.nem.nis.mappers;

import java.util.*;

/**
 * A repository of mappings.
 */
@SuppressWarnings("rawtypes")
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
	public <TSource, TTarget> void addMapping(final Class<? extends TSource> sourceClass, final Class<? super TTarget> targetClass,
			final IMapping<TSource, TTarget> mapping) {
		final MappingTypePair pair = new MappingTypePair(sourceClass, targetClass);
		if (null != this.knownMappings.putIfAbsent(pair, mapping)) {
			throw new MappingException(String.format("cannot change mapping for pair: %s", pair));
		}
	}

	/**
	 * Gets the number of known mappings.
	 *
	 * @return The number of known mappings.
	 */
	public int size() {
		return this.knownMappings.size();
	}

	/**
	 * Gets a value indicating whether or not the desired mapping is supported.
	 *
	 * @param sourceClass The source class.
	 * @param targetClass The target class.
	 * @return true if the mapping is supported.
	 */
	public boolean isSupported(final Class<?> sourceClass, final Class<?> targetClass) {
		final MappingTypePair pair = new MappingTypePair(sourceClass, targetClass);
		return this.knownMappings.containsKey(pair);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <TSource, TTarget> TTarget map(final TSource source, final Class<TTarget> target) {
		final MappingTypePair pair = new MappingTypePair(source.getClass(), target);
		final IMapping mapping = this.knownMappings.getOrDefault(pair, null);

		if (null == mapping) {
			throw new MappingException(String.format("cannot find mapping for pair: %s", pair));
		}

		return (TTarget) mapping.map(source);
	}
}
