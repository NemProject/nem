package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AddressEncoding;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

// TODO-CR: J->G please remember to comment the public api eventually

public class ImportanceTransfer extends Transaction {
	private final int mode;
	private final Address remoteAddress;

	public Address getRemote() {
		return remoteAddress;
	}

	public int getDirection() {
		return mode;
	}

	public ImportanceTransfer(final TimeInstant timeStamp, final Account sender, final int mode, final Address remoteAddress) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, 1, timeStamp, sender);
		this.mode = mode;
		this.remoteAddress = remoteAddress;

		if (null == this.remoteAddress) {
			throw new IllegalArgumentException("remoteAddress is required");
		}

		if (this.mode != ImportanceTransferDirection.Transfer && this.mode != ImportanceTransferDirection.Revert) {
			throw new IllegalArgumentException("invalid mode");
		}
	}

	public ImportanceTransfer(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.IMPORTANCE_TRANSFER, options, deserializer);
		this.mode = deserializer.readInt("mode");
		this.remoteAddress = Address.readFrom(deserializer, "remoteAddress", AddressEncoding.PUBLIC_KEY);

		// TODO-CR: J->G consider adding a validate function or something that you call from both ctors
		// since you probably want to validate the mode in both places too

		// TODO-CR: J->G this is a note for me that Address.readFrom[AddressEncoding.PUBLIC_KEY]
		// is named inconsistently

		if (null == this.remoteAddress) {
			throw new IllegalArgumentException("remoteAddress is required");
		}
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
