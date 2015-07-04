package org.nem.core.model;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * A transaction that creates a mosaic.
 */
public class MosaicCreationTransaction extends Transaction {
	private final Mosaic mosaic;

	/**
	 * Creates a new mosaic creation transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaic The mosaic.
	 */
	public MosaicCreationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Mosaic mosaic) {
		super(TransactionTypes.MOSAIC_CREATION, 1, timeStamp, sender);
		this.mosaic = mosaic;
		this.validate();
	}

	/**
	 * Deserializes a mosaic creation transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MosaicCreationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MOSAIC_CREATION, options, deserializer);
		this.mosaic = deserializer.readObject("mosaic", Mosaic::new);
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaic, "mosaic");

		if (!this.getSigner().equals(this.mosaic.getCreator())) {
			throw new IllegalArgumentException("transaction signer and mosaic creator must be identical");
		}
	}

	/**
	 * Gets the mosaic.
	 *
	 * @return The mosaic.
	 */
	public Mosaic getMosaic() {
		return this.mosaic;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("mosaic", this.mosaic);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new MosaicCreationNotification(this.getMosaic()));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getDebtor(), this.getFee()));
	}
}
