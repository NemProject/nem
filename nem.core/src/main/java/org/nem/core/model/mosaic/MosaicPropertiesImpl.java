package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * Class holding properties of a mosaic.
 * TODO 20150703 J-J: should rename to Default*
 */
public class MosaicPropertiesImpl implements MosaicProperties {
	private final NemProperties properties;

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The properties.
	 */
	public MosaicPropertiesImpl(final Properties properties) {
		MustBe.notNull(properties, "properties");
		this.properties = new NemProperties(properties);
		this.validateProperties();
	}

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The list of nem property objects.
	 */
	public MosaicPropertiesImpl(final Collection<NemProperty> properties) {
		final Properties props = new Properties();
		properties.stream().forEach(p -> props.put(p.getName(), p.getValue()));
		this.properties = new NemProperties(props);
		this.validateProperties();
	}

	@Override
	public int getDivisibility() {
		return this.properties.getOptionalInteger("divisibility", 0);
	}

	@Override
	public boolean isQuantityMutable() {
		return this.properties.getOptionalBoolean("mutablequantity", false);
	}

	@Override
	public boolean isTransferable() {
		return this.properties.getOptionalBoolean("transferable", true);
	}

	@Override
	public Collection<NemProperty> asCollection() {
		return Arrays.asList(
				new NemProperty("divisibility", Integer.toString(this.getDivisibility())),
				new NemProperty("mutablequantity", Boolean.toString(this.isQuantityMutable())),
				new NemProperty("transferable", Boolean.toString(this.isTransferable())));
	}

	private void validateProperties() {
		final int maxDivisibility = 6;
		MustBe.inRange(this.getDivisibility(), "divisibility", 0, maxDivisibility);
	}
}
