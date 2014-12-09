package org.nem.nis.mappers;

import java.lang.reflect.Type;
import java.util.*;

public class AutoMapper {
	private final Map<Class, Map<Class, IMapping>> knownMappings = new HashMap<>();

	public <TSource, TTarget> void addMapping(
			final Class<TSource> sourceClass,
			final Class<TTarget> targetClass,
			IMapping<TSource, TTarget> mapping) {
		final Type[] types = mapping.getClass().getGenericInterfaces();
		final Map<Class, IMapping> sourceMappings = new HashMap<>();
		sourceMappings.put(targetClass, mapping);
		this.knownMappings.put(sourceClass, sourceMappings);
		//this.knownMappings.

	}

	@SuppressWarnings("unchecked")
	public <TSource, TTarget> TTarget map(final TSource source, final Class<TTarget> target) {
		final Class<?> s = source.getClass();

		final Map<Class, IMapping> sourceMappings = this.knownMappings.get(s);

		final IMapping mapping = sourceMappings.get(target);
		return (TTarget)mapping.map(source);
	}
}
