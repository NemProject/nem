package org.nem.core.model;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.MustBe;

import java.util.*;

/**
 * A transaction that creates a mosaic definition.
 */
public class MosaicDefinitionCreationTransaction extends Transaction {
	private final MosaicDefinition mosaicDefinition;

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
		super(TransactionTypes.MOSAIC_DEFINITION_CREATION, 1, timeStamp, sender);
		this.mosaicDefinition = mosaicDefinition;
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
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.mosaicDefinition, "mosaicDefinition");

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

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("mosaicDefinition", this.mosaicDefinition);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		observer.notify(new MosaicDefinitionCreationNotification(this.getMosaicDefinition()));
		super.transfer(observer);
	}
}
