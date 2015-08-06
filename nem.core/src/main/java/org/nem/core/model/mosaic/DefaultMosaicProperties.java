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

	// TODO 20150731 J-B: i think it would be better to have all of these in a sub object (e.g. MosaicTransferFeeInfo) since they're all related
	// > in the case of none, the object could be null; this would avoid conditionally checking if the transfer fee is
	// > present in this object

	// TODO 20150731 J-B: maybe isTransferFeeEnabled since all the other boolean properties start with is?
	// TODO 20150801 BR -> J: ok. do we need that field at all? If we have a separate object for the fee we can check for (non-)null
	@Override
	public boolean isTransferFeeEnabled() {
		return this.properties.getOptionalBoolean("transferFeeEnabled", false);
	}

	@Override
	public Collection<NemProperty> asCollection() {
		final List<NemProperty> nemProperties = new ArrayList<>();
		nemProperties.add(new NemProperty("divisibility", Integer.toString(this.getDivisibility())));
		nemProperties.add(new NemProperty("initialSupply", Long.toString(this.getInitialSupply())));
		nemProperties.add(new NemProperty("supplyMutable", Boolean.toString(this.isSupplyMutable())));
		nemProperties.add(new NemProperty("transferable", Boolean.toString(this.isTransferable())));
		nemProperties.add(new NemProperty("transferFeeEnabled", Boolean.toString(this.isTransferFeeEnabled())));

		return nemProperties;
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
