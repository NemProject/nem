package org.nem.nis.controller.viewmodels;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * View model composed of information about an expired mosaic, including all balances at time of expiry.
 */
public class ExpiredMosaicViewModel implements SerializableEntity {
	private final MosaicId mosaicId;
	private final List<AddressBalancePair> balances;

	/**
	 * Creates a new expired mosaic view model.
	 *
	 * @param mosaicId Expired mosaic id.
	 * @param mosaicBalances Balances at time of expiration.
	 */
	public ExpiredMosaicViewModel(final MosaicId mosaicId, final ReadOnlyMosaicBalances mosaicBalances) {
		this.mosaicId = mosaicId;
		this.balances = mosaicBalances.getOwners()
			.stream()
			.map(address -> new AddressBalancePair(address, mosaicBalances.getBalance(address)))
			.collect(Collectors.toList());
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("mosaicId", this.mosaicId);
		serializer.writeObjectArray("balances", this.balances);
	}

	private static class AddressBalancePair implements SerializableEntity {
		private final Address address;
		private final Quantity quantity;

		public AddressBalancePair(final Address address, final Quantity quantity) {
			this.address = address;
			this.quantity = quantity;
		}

		@Override
		public void serialize(final Serializer serializer) {
			Address.writeTo(serializer, "address", this.address);
			Quantity.writeTo(serializer, "quantity", this.quantity);
		}
	}
}
