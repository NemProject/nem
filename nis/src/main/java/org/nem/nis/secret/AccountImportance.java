package org.nem.nis.secret;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Encapsulates management of an account's importance.
 */
public class AccountImportance implements SerializableEntity {
	private static final Logger LOGGER = Logger.getLogger(AccountImportance.class.getName());

	private final HistoricalOutlinks historicalOutlinks;

	private BlockHeight importanceHeight;
	private double importance;
	private double lastPageRank;

	public static final ObjectDeserializer<AccountImportance> DESERIALIZER =
			deserializer -> new AccountImportance(deserializer);

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

		final boolean isSet = 0 != deserializer.readInt("isSet");
		if (isSet) {
			this.importance = deserializer.readDouble("score");
			this.lastPageRank = deserializer.readDouble("ev");
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
	public void setImportance(final BlockHeight blockHeight, final double importance) {
		// don't allow importance to be reset at the same height
		// TODO 20141007 J-G: do you remember why we have this check?
		if (null != this.importanceHeight && 0 == this.importanceHeight.compareTo(blockHeight)) {
			throw new IllegalArgumentException("importance already set at given height");
		}

		this.importanceHeight = blockHeight;
		this.importance = importance;
	}

	/**
	 * Gets the last page rank.
	 *
	 * @return The last page rank.
	 */
	public double getLastPageRank() {
		return this.lastPageRank;
	}

	/**
	 * Sets the last page rank.
	 *
	 * @param lastPageRank The last page rank.
	 */
	public void setLastPageRank(final double lastPageRank) {
		this.lastPageRank = lastPageRank;
	}

	/**
	 * Gets the page rank at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The importance.
	 */
	public double getImportance(final BlockHeight blockHeight) {
		if (this.importanceHeight == null) {
			LOGGER.warning("your balance hasn't vested yet, harvesting does not make sense");
			return 0.0;
		}
		if (0 != this.importanceHeight.compareTo(blockHeight)) {
			throw new IllegalArgumentException("importance not set at wanted height");
		}

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
		copy.lastPageRank = this.lastPageRank;
		return copy;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("isSet", this.isSet() ? 1 : 0);
		if (this.isSet()) {
			serializer.writeDouble("score", this.importance);
			serializer.writeDouble("ev", this.lastPageRank);
			BlockHeight.writeTo(serializer, "height", this.importanceHeight);
		}
	}

	@Override
	public String toString() {
		final DecimalFormat formatter = FormatUtils.getDecimalFormat(6);
		return String.format("(%s : %s)", this.getHeight(), formatter.format(this.importance));
	}
}
