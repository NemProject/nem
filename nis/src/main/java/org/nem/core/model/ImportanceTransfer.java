package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AddressEncoding;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

import java.util.function.BiPredicate;

public class ImportanceTransfer extends Transaction {
	private final int mode;
	// TODO-CR: J->G how about address (it has a public key)
	// 20140827 G-J: y, I think Address will be ok
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
		// TODO-CR: J->G consider adding a test that the min balance is 1 (although, 1 might be too low)
		return Amount.fromNem(1);
	}
}
