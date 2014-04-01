package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * A mock Transaction implementation.
 */
public class MockTransaction extends Transaction {

	public static final int TYPE = 124;
	public static final int VERSION = 758;
	public static final TimeInstant TIMESTAMP = new TimeInstant(1122448);

	private int customField;
	private long minimumFee;

	/**
	 * Creates a mock transaction.
	 */
	public MockTransaction() {
		this(Utils.generateRandomAccount());
	}

	/**
	 * Creates a mock transaction.
	 *
	 * @param sender The transaction sender's account.
	 */
	public MockTransaction(final Account sender) {
		this(sender, 0);
	}

	/**
	 * Creates a mock transaction.
	 *
	 * @param sender      The transaction sender's account.
	 * @param customField The initial custom field value.
	 */
	public MockTransaction(final Account sender, final int customField) {
		super(TYPE, VERSION, TIMESTAMP, sender);
		this.customField = customField;
	}

	/**
	 * Creates a mock transaction.
	 * This overload is intended to be used for comparison tests.
	 *
	 * @param type      The transaction type.
	 * @param version   The transaction version.
	 * @param timeStamp The transaction timestamp.
	 * @param fee       The transaction fee.
	 */
	public MockTransaction(final int type, final int version, final TimeInstant timeStamp, final long fee) {
		super(type, version, timeStamp, Utils.generateRandomAccount());
		this.setFee(new Amount(fee));
	}

	/**
	 * Deserializes a MockTransaction.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public MockTransaction(final Deserializer deserializer) {
		super(deserializer.readInt("type"), DeserializationOptions.VERIFIABLE, deserializer);
		this.customField = deserializer.readInt("customField");
	}

	/**
	 * Gets the custom field value.
	 *
	 * @return The custom field value.
	 */
	public int getCustomField() {
		return this.customField;
	}

	/**
	 * Sets the minimum fee.
	 *
	 * @param minimumFee The desired minimum fee.
	 */
	public void setMinimumFee(final long minimumFee) {
		this.minimumFee = minimumFee;
	}

	@Override
	public boolean isValid() {
		return super.isValid();
	}

	@Override
	protected Amount getMinimumFee() {
		return new Amount(this.minimumFee);
	}

	@Override
	protected void serializeImpl(Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("customField", this.customField);
	}

	@Override
	public void execute() {
	}
}