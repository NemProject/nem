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
	 * Gets the mosaic transfers.
	 *
	 * @return The mosaic transfers.
	 */
	public Collection<MosaicTransferPair> getMosaicTransfers() {
		return this.mosaicTransfers.entrySet().stream()
				.map(e -> new MosaicTransferPair(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Adds a mosaic transfer.
	 *
	 * @param pair The mosaic transfer pair.
	 */
	public void addMosaicTransfer(final MosaicTransferPair pair) {
		this.addMosaicTransfer(pair.getMosaicId(), pair.getQuantity());
	}

	/**
	 * Adds a mosaic transfer.
	 *
	 * @param mosaicId The mosaic id.
	 * @param quantity The quantity.
	 */
	public void addMosaicTransfer(final MosaicId mosaicId, final Quantity quantity) {
		final Quantity originalQuantity = this.mosaicTransfers.getOrDefault(mosaicId, Quantity.ZERO);
		this.mosaicTransfers.put(mosaicId, originalQuantity.add(quantity));
	}
}