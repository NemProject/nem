package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.utils.MustBe;

import java.math.BigInteger;
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
	public boolean hasTransferFee() {
		return this.properties.getOptionalBoolean("hasTransferFee", false);
	}

	@Override
	public boolean isTransferFeeAbsolute() {
		if (!this.hasTransferFee()) {
			throw new UnsupportedOperationException("mosaic has no transfer fee");
		}

		return this.properties.getBoolean("absoluteTransferFee");
	}
	@Override
	public Address getTransferFeeRecipient() {
		if (!this.hasTransferFee()) {
			throw new UnsupportedOperationException("mosaic has no transfer fee");
		}

		return Address.fromEncoded(this.properties.getString("transferFeeRecipient"));
	}

	@Override
	public long getTransferFee() {
		if (!this.hasTransferFee()) {
			throw new UnsupportedOperationException("mosaic has no transfer fee");
		}

		return this.properties.getLong("transferFee");
	}

	@Override
	public Collection<NemProperty> asCollection() {
		final List<NemProperty> nemProperties = new ArrayList<>();
		nemProperties.add(new NemProperty("divisibility", Integer.toString(this.getDivisibility())));
		nemProperties.add(new NemProperty("initialSupply", Long.toString(this.getInitialSupply())));
		nemProperties.add(new NemProperty("supplyMutable", Boolean.toString(this.isSupplyMutable())));
		nemProperties.add(new NemProperty("transferable", Boolean.toString(this.isTransferable())));
		nemProperties.add(new NemProperty("hasTransferFee", Boolean.toString(this.hasTransferFee())));
		if (this.hasTransferFee()) {
			nemProperties.add(new NemProperty("absoluteTransferFee", Boolean.toString(this.isTransferFeeAbsolute())));
			nemProperties.add(new NemProperty("transferFeeRecipient", this.getTransferFeeRecipient().toString()));
			nemProperties.add(new NemProperty("transferFee", Long.toString(this.getTransferFee())));
		}

		return nemProperties;
	}

	private void validateProperties() {
		final int divisibility = this.getDivisibility();
		MustBe.inRange(divisibility, "divisibility", 0, 6);

		// note that MosaicUtils.add will throw if quantity is too large
		MosaicUtils.add(divisibility, Supply.ZERO, new Supply(this.getInitialSupply()));
		if (this.hasTransferFee()) {
			final long powerOfTen = BigInteger.TEN.pow(divisibility).longValue();
			if (!this.isTransferFeeAbsolute() && (powerOfTen * this.getInitialSupply()) % 10_000 != 0) {
				throw new IllegalArgumentException("initial supply and divisibility not compatible to transfer fee");
			}

			if (!this.getTransferFeeRecipient().isValid()) {
				throw new IllegalArgumentException("transfer fee recipient is not a valid address");
			}
		}
	}
}
