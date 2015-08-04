package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Supply;
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
	public long getInitialSupply() {
		return this.properties.getOptionalLong("initialSupply", 1_000L);
	}

	@Override
	public boolean isSupplyMutable() {
		return this.properties.getOptionalBoolean("supplyMutable", false);
	}

	@Override
	public boolean isTransferable() {
		return this.properties.getOptionalBoolean("transferable", true);
	}

	@Override
	public Collection<NemProperty> asCollection() {
		return Arrays.asList(
				new NemProperty("divisibility", Integer.toString(this.getDivisibility())),
				new NemProperty("initialSupply", Long.toString(this.getInitialSupply())),
				new NemProperty("supplyMutable", Boolean.toString(this.isSupplyMutable())),
				new NemProperty("transferable", Boolean.toString(this.isTransferable())));
	}

	private void validateProperties() {
		final int divisibility = this.getDivisibility();
		MustBe.inRange(divisibility, "divisibility", 0, 6);

		// note that MosaicUtils.add will throw if quantity is too large
		MosaicUtils.add(divisibility, Supply.ZERO, new Supply(this.getInitialSupply()));
	}

	@Override
	public int hashCode() {
		return this.asCollection().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof DefaultMosaicProperties)) {
			return false;
		}

		final DefaultMosaicProperties rhs = (DefaultMosaicProperties)obj;
		return this.asCollection().equals(rhs.asCollection());
	}
}