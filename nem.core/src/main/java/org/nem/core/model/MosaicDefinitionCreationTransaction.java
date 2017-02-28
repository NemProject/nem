package org.nem.core.model;

import org.nem.core.model.mosaic.*;
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
	private final Account creationFeeSink;
	private final Amount creationFee;

	/**
	 * Creates a new mosaic definition creation transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaicDefinition The mosaic definition.
	 */
	public MosaicDefinitionCreationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final MosaicDefinition mosaicDefinition) {
		this(timeStamp, sender, mosaicDefinition, MosaicConstants.MOSAIC_CREATION_FEE_SINK, Amount.fromNem(50000));
	}

	/**
	 * Creates a new mosaic definition creation transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param sender The sender.
	 * @param mosaicDefinition The mosaic definition.
	 * @param creationFeeSink The creation fee sink.
	 * @param creationFee The creation fee.
	 */
	public MosaicDefinitionCreationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final MosaicDefinition mosaicDefinition,
			final Account creationFeeSink,
			final Amount creationFee) {
		super(TransactionTypes.MOSAIC_DEFINITION_CREATION, 1, timeStamp, sender);
		this.mosaicDefinition = mosaicDefinition;
		this.creationFeeSink = creationFeeSink;
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
		this.creationFeeSink = Account.readFrom(deserializer, "creationFeeSink");
		this.creationFee = Amount.readFrom(deserializer, "creationFee");
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaicDefinition, "mosaicDefinition");
		MustBe.notNull(this.creationFeeSink, "creationFeeSink");
		MustBe.notNull(this.creationFee, "creationFee");

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
	 * Gets the mosaic creation fee sink.
	 *
	 * @return The mosaic creation fee sink.
	 */
	public Account getCreationFeeSink() {
		return this.creationFeeSink;
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
		final Collection<Account> accounts = new ArrayList<>();
		accounts.add(this.creationFeeSink);

		final MosaicLevy levy = this.getMosaicDefinition().getMosaicLevy();
		if (null != levy && !levy.getRecipient().equals(this.getSigner())) {
			accounts.add(levy.getRecipient());
		}

		return accounts;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("mosaicDefinition", this.mosaicDefinition);
		Account.writeTo(serializer, "creationFeeSink", this.creationFeeSink);
		Amount.writeTo(serializer, "creationFee", this.creationFee);
	}

	@Override
	protected void transfer(final TransactionObserver observer, final TransactionExecutionState state) {
		observer.notify(new MosaicDefinitionCreationNotification(this.getMosaicDefinition()));
		observer.notify(new BalanceTransferNotification(this.getSigner(), this.creationFeeSink, this.creationFee));
		super.transfer(observer, state);
	}
}
