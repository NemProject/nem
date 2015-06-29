package org.nem.core.model.mosaic;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;

import java.util.*;

/**
 * Class defining a mosaic.
 */
public class Mosaic implements MosaicProperties {
	private final Account creator;
	private final MosaicProperties properties;
	private final List<Mosaic> children = Collections.emptyList();

	/**
	 * Creates a new mosaic.
	 *
	 * @param creator The creator of the mosaic.
	 * @param properties The properties of the mosaic.
	 */
	public Mosaic(final Account creator, final Properties properties) {
		this(creator, new MosaicPropertiesImpl(properties));
	}

	private Mosaic(final Account creator, final MosaicProperties properties) {
		if (null == creator) {
			throw new IllegalArgumentException("creator of the mosaic cannot be null");
		}

		if (null == properties) {
			throw new IllegalArgumentException("properties of the mosaic cannot be null");
		}

		this.creator = creator;
		this.properties = properties;
	}

	/**
	 * Gets the creator of the mosaic.
	 *
	 * @return the creator.
	 */
	public Account getCreator() {
		return this.creator;
	}

	public List<Mosaic> getChildren() {
		return this.children;
	}

	@Override
	public String getName() {
		return this.properties.getName();
	}

	@Override
	public String getDescription() {
		return this.properties.getDescription();
	}

	@Override
	public int getDivisibility() {
		return this.properties.getDivisibility();
	}

	@Override
	public NamespaceId getNamespaceId() {
		return this.properties.getNamespaceId();
	}

	@Override
	public boolean isQuantityMutable() {
		return this.properties.isQuantityMutable();
	}

	@Override
	public boolean isTransferable() {
		return this.properties.isTransferable();
	}
}
