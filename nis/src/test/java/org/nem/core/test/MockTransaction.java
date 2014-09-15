package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.model.observers.TransferObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.*;

/**
 * A mock Transaction implementation.
 */
public class MockTransaction extends Transaction {

	public static final int TYPE = 124;
	public static final int VERSION = 758;
	public static final TimeInstant TIMESTAMP = new TimeInstant(1122448);
	public static final TimeInstant DEADLINE = TIMESTAMP.addHours(2);

	private int customField;
	private long minimumFee;

	private List<Integer> executeList = new ArrayList<>();
	private List<Integer> undoList = new ArrayList<>();
	private Consumer<TransferObserver> transferAction = to -> { };

	private int numExecuteCommitCalls;
	private int numUndoCommitCalls;
	private int numTransferCalls;

	private ValidationResult validationResult = ValidationResult.SUCCESS;

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
	 * @param sender The transaction sender's account.
	 * @param customField The initial custom field value.
	 */
	public MockTransaction(final Account sender, final int customField) {
		super(TYPE, VERSION, TIMESTAMP, sender);
		this.customField = customField;
		this.setDeadline(DEADLINE);
	}

	/**
	 * Creates a mock transaction.
	 *
	 * @param customField The initial custom field value.
	 * @param timeStamp The transaction timestamp.
	 */
	public MockTransaction(final int customField, final TimeInstant timeStamp) {
		this(Utils.generateRandomAccount(), customField, timeStamp);
	}

	/**
	 * Creates a mock transaction.
	 *
	 * @param sender The transaction sender's account.
	 * @param customField The initial custom field value.
	 * @param timeStamp The transaction timestamp.
	 */
	public MockTransaction(final Account sender, final int customField, final TimeInstant timeStamp) {
		super(TYPE, VERSION, timeStamp, sender);
		this.customField = customField;
		this.setDeadline(timeStamp.addHours(2));
	}

	/**
	 * Creates a mock transaction.
	 * This overload is intended to be used for comparison tests.
	 *
	 * @param type The transaction type.
	 * @param version The transaction version.
	 * @param timeStamp The transaction timestamp.
	 * @param fee The transaction fee.
	 */
	public MockTransaction(final int type, final int version, final TimeInstant timeStamp, final long fee) {
		super(type, version, timeStamp, Utils.generateRandomAccount());
		this.setFee(new Amount(fee));
		this.setDeadline(timeStamp.addHours(2));
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
	 * Gets the number executeCommit was called.
	 *
	 * @return The number of times executeCommit was called.
	 */
	public int getNumExecuteCommitCalls() {
		return this.numExecuteCommitCalls;
	}

	/**
	 * Gets the number undoCommit was called.
	 *
	 * @return The number of times undoCommit was called.
	 */
	public int getNumUndoCommitCalls() {
		return this.numUndoCommitCalls;
	}

	/**
	 * Gets the number of times transfer was called.
	 *
	 * @return The number of times transfer was called.
	 */
	public int getNumTransferCalls() {
		return this.numTransferCalls;
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

	/**
	 * Sets a list that this transaction should add its custom field to when execute is called.
	 *
	 * @param list The list.
	 */
	public void setExecuteList(final List<Integer> list) {
		this.executeList = list;
	}

	/**
	 * Sets a list that this transaction should add its custom field to when undo is called.
	 *
	 * @param list The list.
	 */
	public void setUndoList(final List<Integer> list) {
		this.undoList = list;
	}

	/**
	 * Sets an action that should be executed when transfer is called.
	 *
	 * @param transferAction The action.
	 */
	public void setTransferAction(final Consumer<TransferObserver> transferAction) {
		this.transferAction = transferAction;
	}

	/**
	 * Sets the validation result that should be returned from checkDerivedValidity.
	 *
	 * @param validationResult The validation result.
	 */
	public void setValidationResult(final ValidationResult validationResult) {
		this.validationResult = validationResult;
	}

	@Override
	public ValidationResult checkDerivedValidity(final BiPredicate<Account, Amount> canDebitPredicate) {
		return this.validationResult;
	}

	@Override
	protected Amount getMinimumFee() {
		return new Amount(this.minimumFee);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeInt("customField", this.customField);
	}

	@Override
	protected void executeCommit() {
		++this.numExecuteCommitCalls;
		this.executeList.add(this.customField);
	}

	@Override
	protected void undoCommit() {
		++this.numUndoCommitCalls;
		this.undoList.add(this.customField);
	}

	@Override
	protected void transfer(final TransferObserver observer) {
		this.transferAction.accept(observer);
		++this.numTransferCalls;
	}
}