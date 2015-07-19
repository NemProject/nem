package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * Class holding properties of a mosaic.
 */
public class DefaultMosaicProperties implements MosaicProperties {
	private final NemProperties properties;

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The properties.
	 */
	public DefaultMosaicProperties(final Properties properties) {
		MustBe.notNull(properties, "properties");
		this.properties = new NemProperties(properties);
		this.validateProperties();
	}

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The list of nem property objects.
	 */
	public DefaultMosaicProperties(final Collection<NemProperty> properties) {
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
	public long getInitialQuantity() {
		return this.properties.getOptionalLong("quantity", 1_000L);
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
				new NemProperty("quantity", Long.toString(this.getInitialQuantity())),
				new NemProperty("mutablequantity", Boolean.toString(this.isQuantityMutable())),
				new NemProperty("transferable", Boolean.toString(this.isTransferable())));
	}

	private void validateProperties() {
		final int divisibility = this.getDivisibility();
		MustBe.inRange(divisibility, "divisibility", 0, 6);

		// note that MosaicUtils.add will throw if quantity is too large
		MosaicUtils.add(divisibility, Quantity.ZERO, new Quantity(this.getInitialQuantity()));
	}
}
