package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AddressEncoding;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

// TODO-CR: J->G please remember to comment the public api eventually

public class ImportanceTransferTransaction extends Transaction {
	private final int mode;
	private final Address remoteAddress;

	public Address getRemote() {
		return remoteAddress;
	}

	public int getDirection() {
		return mode;
	}

	public ImportanceTransferTransaction(final TimeInstant timeStamp, final Account sender, final int mode, final Address remoteAddress) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, 1, timeStamp, sender);
		this.mode = mode;
		this.remoteAddress = remoteAddress;

		if (null == this.remoteAddress) {
			throw new IllegalArgumentException("remoteAddress is required");
		}

		if (this.mode != ImportanceTransferTransactionDirection.Transfer && this.mode != ImportanceTransferTransactionDirection.Revert) {
			throw new IllegalArgumentException("invalid mode");
		}
	}

	public ImportanceTransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, options, deserializer);
		this.mode = deserializer.readInt("mode");
		this.remoteAddress = Address.readFrom(deserializer, "remoteAddress", AddressEncoding.PUBLIC_KEY);

		// TODO-CR: J->G do you want to validate the mode here?
		// G->J any reason not to? (would checkDerivedValidity be better place?)
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("mode", this.mode);
		Address.writeTo(serializer, "remoteAddress", this.remoteAddress, AddressEncoding.PUBLIC_KEY);
	}

	@Override
	protected void executeCommit() {
		// empty
	}

	@Override
	protected void undoCommit() {
		// empty
	}

	@Override
	protected void transfer(final TransferObserver observer) {
		observer.notifyDebit(this.getSigner(), this.getFee());
	}

	@Override
	protected ValidationResult checkDerivedValidity(final BiPredicate<Account, Amount> canDebitPredicate) {
		if (!canDebitPredicate.test(this.getSigner(), this.getFee())) {
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}
		return ValidationResult.SUCCESS;
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.fromNem(1);
	}
}
