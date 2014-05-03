package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.messages.MessageFactory;
import org.nem.core.serialization.*;

import java.util.*;

/**
 * A NEM account.
 */
public class Account implements SerializableEntity {

	private final KeyPair keyPair;
	private final Address address;
	private final List<Message> messages;
	private String label;
	private Amount balance = Amount.ZERO;
	
	private List<AccountLink> inlinks;
	private List<AccountLink> outlinks;
	
	private CoinDays coindays;

	private BlockAmount foragedBlocks;
	private HistoricalBalances historicalBalances;

	/**
	 * Creates an account around a key pair.
	 *
	 * @param keyPair The key pair.
	 */
	public Account(final KeyPair keyPair) {
		this(keyPair, Address.fromPublicKey(keyPair.getPublicKey()));
	}

	/**
	 * Creates an account around an address. This constructor should only
	 * be used if the account's public key is not known.
	 *
	 * @param address The address.
	 */
	public Account(final Address address) {
		this(null == address.getPublicKey() ? null : new KeyPair(address.getPublicKey()), address);
	}

	/**
	 * Creates an account around a key pair and address.
	 *
	 * @param keyPair The key pair.
	 * @param address The address.
	 */
	protected Account(final KeyPair keyPair, final Address address) {
		this.keyPair = keyPair;
		this.address = address;
		this.messages = new ArrayList<>();
		this.foragedBlocks = BlockAmount.ZERO;
		this.historicalBalances = new HistoricalBalances();
	}

	/**
	 * Deserializes an account.
	 *
	 * @param deserializer The deserializer.
	 */
	public Account(final Deserializer deserializer) {
		this(deserializeAddress(deserializer));

		this.balance = Amount.readFrom(deserializer, "balance");
		this.foragedBlocks = BlockAmount.readFrom(deserializer, "foragedBlocks");
		this.label = deserializer.readString("label");
		this.messages.addAll(deserializer.readObjectArray("messages", MessageFactory.createDeserializer(this, this)));
	}

	private static Address deserializeAddress(final Deserializer deserializer) {
		final Address addressWithoutPublicKey = readAddress(deserializer, "address", AccountEncoding.ADDRESS);
		final Address addressWithPublicKey = readAddress(deserializer, "publicKey", AccountEncoding.PUBLIC_KEY);
		return null != addressWithPublicKey ? addressWithPublicKey : addressWithoutPublicKey;
	}

	@Override
	public void serialize(final Serializer serializer) {
		writeTo(serializer, "address", this, AccountEncoding.ADDRESS);
		writeTo(serializer, "publicKey", this, AccountEncoding.PUBLIC_KEY);

		Amount.writeTo(serializer, "balance", this.getBalance());
		BlockAmount.writeTo(serializer, "foragedBlocks", this.getForagedBlocks());
		serializer.writeString("label", this.getLabel());
		serializer.writeObjectArray("messages", this.getMessages());
	}

	/**
	 * Gets the account's key pair.
	 *
	 * @return This account's key pair.
	 */
	public KeyPair getKeyPair() {
		return this.keyPair;
	}

	/**
	 * Gets the account's address.
	 *
	 * @return This account's address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	/**
	 * Adds amount to the account's balance.
	 *
	 * @param amount The amount by which to increment the balance.
	 */
	public void incrementBalance(final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	/**
	 * Subtracts amount from the account's balance.
	 *
	 * @param amount The amount by which to decrement the balance.
	 */
	public void decrementBalance(final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	/**
	 * Gets number of foraged blocks.
	 *
	 * @return Number of blocks foraged by this account.
	 */
	public BlockAmount getForagedBlocks() {
		return foragedBlocks;
	}

	/**
	 * Increments number of foraged blocks by this account by one.
	 */
	public void incrementForagedBlocks() {
		this.foragedBlocks = this.foragedBlocks.increment();
	}

	/**
	 * Decrements number of foraged blocks by this account by one.
	 */
	public void decrementForagedBlocks() {
		this.foragedBlocks = this.foragedBlocks.decrement();
	}

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Sets the account's label.
	 *
	 * @param label The desired label.
	 */
	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * Gets all messages associated with the account.
	 *
	 * @return All messages associated with the account.
	 */
	public List<Message> getMessages() {
		return this.messages;
	}

	/**
	 * Associates message with the account.
	 *
	 * @param message The message to associate with this account.
	 */
	public void addMessage(final Message message) {
		this.messages.add(message);
	}
	
	/**
	 * Removes the last occurrence of the specified message from this account.
	 *
	 * @param message The message to remove from this account.
	 */
	public void removeMessage(final Message message) {
		for (int i = this.messages.size() - 1; i >= 0; --i) {
			if (message.equals(this.messages.get(i))) {
				this.messages.remove(i);
				break;
			}
		}
	}

	/**
	 * Gets the historical balance at a given height.
	 *
	 * @return The historical balance.
	 */
	public Amount getBalance(final BlockHeight lastBlockHeight, final BlockHeight height) {
		return historicalBalances.getBalance(lastBlockHeight, height);
	}
	
	/**
	 * Adds a given amount to the historical balances at a given height.
	 *
	 * @param height The block height.
	 * @param amount The amount to add
	 */
	public void addHistoricalBalance(final BlockHeight height, final Amount amount) {
		historicalBalances.add(height, amount);
	}
	
	/**
	 * Subtracts a given amount from the historical balances at a given height.
	 *
	 * @param height The block height.
	 * @param amount The amount to add
	 */
	public void subtractHistoricalBalance(final BlockHeight height, final Amount amount) {
		historicalBalances.subtract(height, amount);
	}

	/**
	 * @param acctLink - an inLink to add
	 */
	public void addInlink(AccountLink acctLink) {
		if (this.inlinks == null) {
			this.inlinks = new LinkedList<AccountLink>();
		}
		this.inlinks.add(acctLink);
	}
	
	/**
	 * @return the inlinks
	 */
	public List<AccountLink> getInlinks() {
		return inlinks;
	}

	/**
	 * @param inlinks the inLinks to set
	 */
	public void setInlinks(List<AccountLink> inlinks) {
		this.inlinks = inlinks;
	}
	
	/**
	 * @param acctLink - an outlink to add
	 */
	public void addOutlink(AccountLink acctLink) {
		if (this.outlinks == null) {
			this.outlinks = new LinkedList<AccountLink>();
		}
		this.outlinks.add(acctLink);
	}
	
	/**
	 * @return the outlinks
	 */
	public List<AccountLink> getOutlinks() {
		return outlinks;
	}

	/**
	 * @param outlinks the outlinks to set
	 */
	public void setOutlinks(List<AccountLink> outlinks) {
		this.outlinks = outlinks;
	}
	
	/**
	 * This method applies the <code>coindays</code> to the balance and returns the result.
	 * 
	 * @return
	 */
	public Amount getCoinDayWeightedBalance() {
		
		long currentBlockHeight = 1337;//XXX:TODO:this is temporary until I figure out a good way to get the current height
		
		long getUnweightedBalance = coindays.getUnweightedBalance();
		
		long coinDayBalance = coindays.getCoinDayWeightedBalance(currentBlockHeight);
		
		//Assume any remaining balance has the full weight
		long remainder = this.getBalance().getNumMicroNem() - getUnweightedBalance;
		coinDayBalance += remainder;
		
		if (coinDayBalance > this.getBalance().getNumMicroNem()) {
			coinDayBalance = this.getBalance().getNumMicroNem(); //XXX:or should we throw an exception?
			throw new IllegalStateException("Calculate coinday balance is greater than the balance.");
		}
		
		return new Amount(coinDayBalance);
	}
	
	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Account))
			return false;

		Account rhs = (Account)obj;
		return this.address.equals(rhs.address);
	}

	//region inline serialization

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label      The optional label.
	 * @param account    The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Account account) {
		writeTo(serializer, label, account, AccountEncoding.ADDRESS);
	}

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label      The optional label.
	 * @param account    The object.
	 * @param encoding   The account encoding mode.
	 */
	public static void writeTo(
			final Serializer serializer,
			final String label,
			final Account account,
			final AccountEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				final KeyPair keyPair = account.getKeyPair();
				serializer.writeBytes(label, null != keyPair ? keyPair.getPublicKey().getRaw() : null);
				break;

			case ADDRESS:
			default:
				Address.writeTo(serializer, label, account.getAddress());
				break;
		}
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label        The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(final Deserializer deserializer, final String label) {
		return readFrom(deserializer, label, AccountEncoding.ADDRESS);
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param encoding     The account encoding.
	 * @param label        The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(
			final Deserializer deserializer,
			final String label,
			final AccountEncoding encoding) {
		final Address address = readAddress(deserializer, label, encoding);
		return deserializer.getContext().findAccountByAddress(address);
	}

	private static Address readAddress(
			final Deserializer deserializer,
			final String label,
			final AccountEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				final byte[] publicKeyBytes = deserializer.readBytes(label);
				return null == publicKeyBytes ? null : Address.fromPublicKey(new PublicKey(publicKeyBytes));

			case ADDRESS:
			default:
				return Address.readFrom(deserializer, label);
		}
	}

	//endregion

	/**
	 * Creates an unlinked copy of this account.
	 *
	 * @return An unlinked copy of this account.
	 */
	public Account copy() {
		final Account copy = new Account(this.getKeyPair(), this.getAddress());
		copy.balance = this.getBalance();
		copy.label = this.getLabel();
		copy.foragedBlocks = this.getForagedBlocks();
		copy.messages.addAll(this.getMessages());
		copy.historicalBalances = this.historicalBalances.copy();
		return copy;
	}
}