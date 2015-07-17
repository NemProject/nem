package org.nem.core.model.mosaic;

import org.nem.core.model.*;
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
	public long getQuantity() {
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
				new NemProperty("quantity", Long.toString(this.getQuantity())),
				new NemProperty("mutablequantity", Boolean.toString(this.isQuantityMutable())),
				new NemProperty("transferable", Boolean.toString(this.isTransferable())));
	}

	private void validateProperties() {
		final int maxDivisibility = 6;
		final long maxQuantity = MosaicProperties.MAX_QUANTITY;
		MustBe.inRange(this.getDivisibility(), "divisibility", 0, maxDivisibility);
		// TODO 20150710 J-B: should we allow quantity to be zero here?
		// TODO 20150711 BR -> J: no, a max quantity of 0 does not make sense since you can never create any smart tiles then
		MustBe.inRange(this.getQuantity(), "quantity", 1L, maxQuantity);
	}
}
