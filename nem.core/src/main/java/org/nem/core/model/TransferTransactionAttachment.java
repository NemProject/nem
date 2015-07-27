package org.nem.core.model;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An attachment that can be attached to a transfer transaction.
 */
public class TransferTransactionAttachment {
	private final Map<MosaicId, Quantity> mosaicTransfers = new HashMap<>();
	private Message message;

	/**
	 * Creates an empty attachment.
	 */
	public TransferTransactionAttachment() {
	}

	/**
	 * Creates an attachment with a message.
	 *
	 * @param message The message.
	 */
	public TransferTransactionAttachment(final Message message) {
		this.setMessage(message);
	}

	/**
	 * Sets the message.
	 *
	 * @param message The message.
	 */
	public void setMessage(final Message message) {
		if (null != this.message) {
			throw new IllegalStateException("cannot reset message");
		}

		this.message = message;
	}

	/**
	 * Gets the message.
	 *
	 * @return The message.
	 */
	public Message getMessage() {
		return this.message;
	}

	/**
	 * Gets the mosaics.
	 *
	 * @return The mosaics.
	 */
	public Collection<Mosaic> getMosaics() {
		return this.mosaicTransfers.entrySet().stream()
				.map(e -> new Mosaic(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Adds a mosaic.
	 *
	 * @param mosaic The mosaic.
	 */
	public void addMosaic(final Mosaic mosaic) {
		this.addMosaic(mosaic.getMosaicId(), mosaic.getQuantity());
	}

	/**
	 * Adds a mosaic.
	 *
	 * @param mosaicId The mosaic id.
	 * @param quantity The quantity.
	 */
	public void addMosaic(final MosaicId mosaicId, final Quantity quantity) {
		final Quantity originalQuantity = this.mosaicTransfers.getOrDefault(mosaicId, Quantity.ZERO);
		this.mosaicTransfers.put(mosaicId, originalQuantity.add(quantity));
	}
}