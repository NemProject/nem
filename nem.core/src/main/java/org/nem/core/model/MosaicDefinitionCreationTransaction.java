package org.nem.core.model;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * A transaction that creates a mosaic definition.
 */
public class MosaicDefinitionCreationTransaction extends Transaction {
	private final MosaicDefinition mosaicDefinition;
	private final Account admitter;
	private final Amount creationFee;

	/**
	 * Creates a new mosaic definition creation transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaicDefinition The mosaic definition.
	 * @param admitter The admitter.
	 * @param creationFee The creation fee.
	 */
	public MosaicDefinitionCreationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final MosaicDefinition mosaicDefinition,
			final Account admitter, // TODO 20150805 J-B: you didn't like removing this parameter from the constructor?
			final Amount creationFee) {
		super(TransactionTypes.MOSAIC_DEFINITION_CREATION, 1, timeStamp, sender);
		this.mosaicDefinition = mosaicDefinition;
		this.admitter = admitter;
		this.creationFee = creationFee;
		this.validate();
	}

	/**
	 * Deserializes a mosaic definition creation transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MosaicDefinitionCreationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MOSAIC_DEFINITION_CREATION, options, deserializer);
		this.mosaicDefinition = deserializer.readObject("mosaicDefinition", MosaicDefinition::new);
		this.admitter = Account.readFrom(deserializer, "admitter", AddressEncoding.PUBLIC_KEY);
		this.creationFee = Amount.readFrom(deserializer, "creationFee");
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaicDefinition, "mosaicDefinition");
		MustBe.notNull(this.admitter, "admitter");
		MustBe.notNull(this.creationFee, "creationFee");

		if (!this.admitter.hasPublicKey()) {
			throw new IllegalArgumentException("admitter public key required");
		}

		if (!this.getSigner().equals(this.mosaicDefinition.getCreator())) {
			throw new IllegalArgumentException("transaction signer and mosaic definition creator must be identical");
		}
	}

	/**
	 * Gets the mosaic definition.
	 *
	 * @return The mosaic definition.
	 */
	public MosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}

	/**
	 * Gets the mosaic admitter.
	 *
	 * @return The mosaic admitter.
	 */
	public Account getAdmitter() {
		return this.admitter;
	}

	/**
	 * Gets the creation fee.
	 *
	 * @return The creation fee.
	 */
	public Amount getCreationFee() {
		return this.creationFee;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.singletonList(this.admitter);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("mosaicDefinition", this.mosaicDefinition);
		Account.writeTo(serializer, "admitter", this.admitter, AddressEncoding.PUBLIC_KEY);
		Amount.writeTo(serializer, "creationFee", this.creationFee);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new MosaicDefinitionCreationNotification(this.getMosaicDefinition()));
		observer.notify(new BalanceTransferNotification(this.getSigner(), this.admitter, this.creationFee));
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getDebtor(), this.getFee()));
	}
}
