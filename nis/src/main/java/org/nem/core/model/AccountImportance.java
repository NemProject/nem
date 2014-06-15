package org.nem.core.model;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Iterator;

/**
 * Encapsulates management of an account's importance.
 */
public class AccountImportance implements SerializableEntity {

	private final HistoricalOutlinks historicalOutlinks;

	private BlockHeight importanceHeight;
	private double importance;

	public static final ObjectDeserializer<AccountImportance> DESERIALIZER = new ObjectDeserializer<AccountImportance>() {
		@Override
		public AccountImportance deserialize(Deserializer deserializer) {
			return new AccountImportance(deserializer);
		}
	};
	/**
	 * Creates a new importance instance.
	 */
	public AccountImportance() {
		this(new HistoricalOutlinks());
	}

	/**
	 * Deserializes an account importance instance.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountImportance(final Deserializer deserializer) {
		this();

		boolean isSet = 0 != deserializer.readInt("isSet");
		if (isSet) {
			this.importance = deserializer.readDouble("score");
			this.importanceHeight = BlockHeight.readFrom(deserializer, "height");
		}
	}

	private AccountImportance(final HistoricalOutlinks historicalOutlinks) {
		this.historicalOutlinks = historicalOutlinks;
	}

	/**
	 * Adds an out-link.
	 *
	 * @param accountLink The account link to add.
	 */
	public void addOutlink(final AccountLink accountLink) {
		this.historicalOutlinks.add(
				accountLink.getHeight(),
				accountLink.getOtherAccountAddress(),
				accountLink.getAmount());
	}

	/**
	 * Removes an out-link.
	 *
	 * @param accountLink The account link to remove.
	 */
	public void removeOutlink(final AccountLink accountLink) {
		this.historicalOutlinks.remove(
				accountLink.getHeight(),
				accountLink.getOtherAccountAddress(),
				accountLink.getAmount());
	}

	/**
	 * Gets an iterator that returns all out-links at or before the (inclusive) given height.
	 *
	 * @param blockHeight The block height.
	 * @return The matching links.
	 */
	public Iterator<AccountLink> getOutlinksIterator(final BlockHeight blockHeight) {
		return this.historicalOutlinks.outlinksIterator(blockHeight);
	}

	/**
	 * Gets the number of out-links at or before the (inclusive) given height.
	 *
	 * @param blockHeight The block height.
	 * @return The number of matching links.
	 */
	public int getOutlinksSize(final BlockHeight blockHeight) {
		return this.historicalOutlinks.outlinksSize(blockHeight);
	}

	/**
	 * Sets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @param importance The importance.
	 */
	public void setImportance(final BlockHeight blockHeight, double importance) {
		if (null == importanceHeight || 0 != this.importanceHeight.compareTo(blockHeight)) {
			this.importanceHeight = blockHeight;
			this.importance = importance;

		} else if (this.importanceHeight.compareTo(blockHeight) != 0) {
			throw new IllegalArgumentException("importance already set at given height");
		}
	}

	/**
	 * Gets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The importance.
	 */
	public double getImportance(final BlockHeight blockHeight) {
		if (this.importanceHeight == null || 0 != this.importanceHeight.compareTo(blockHeight))
			throw new IllegalArgumentException("importance not set at wanted height");

		return this.importance;
	}

	/**
	 * Gets the height at which importance is set.
	 *
	 * @return The height of importance.
	 */
	public BlockHeight getHeight() {
		return this.importanceHeight;
	}

	/**
	 * Gets a value indicating whether or not the importance is set.
	 *
	 * @return true if the importance is set.
	 */
	public boolean isSet() {
		return null != this.importanceHeight;
	}

	/**
	 * Creates a copy of this importance.
	 *
	 * @return A copy of this importance.
	 */
	public AccountImportance copy() {
		final AccountImportance copy = new AccountImportance(this.historicalOutlinks.copy());
		copy.importance = this.importance;
		copy.importanceHeight = this.importanceHeight;
		return copy;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("isSet", this.isSet() ? 1 : 0);
		if (this.isSet()) {
			serializer.writeDouble("score", this.importance);
			BlockHeight.writeTo(serializer, "height", this.importanceHeight);
		}
	}

	@Override
	public String toString() {
		final DecimalFormat formatter = FormatUtils.getDecimalFormat(6);
		return String.format("(%s : %s)", this.getHeight(), formatter.format(this.importance));
	}
}
