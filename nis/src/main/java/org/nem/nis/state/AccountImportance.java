package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Encapsulates management of an account's importance.
 */
public class AccountImportance implements ReadOnlyAccountImportance {
	private static final Logger LOGGER = Logger.getLogger(AccountImportance.class.getName());

	private final HistoricalOutlinks historicalOutlinks;

	private BlockHeight importanceHeight;
	private double importance;
	private double lastPageRank;

	/**
	 * Creates a new importance instance.
	 */
	public AccountImportance() {
		this(new HistoricalOutlinks());
	}

	/**
	 * Creates a new importance instance.
	 *
	 * @param height The block height.
	 * @param importance The importance.
	 * @param lastPageRank The last page rank.
	 */
	public AccountImportance(final BlockHeight height, final double importance, final double lastPageRank) {
		this();
		this.importanceHeight = height;
		this.importance = importance;
		this.lastPageRank = lastPageRank;
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
		this.historicalOutlinks.add(accountLink.getHeight(), accountLink.getOtherAccountAddress(), accountLink.getAmount());
	}

	/**
	 * Removes an out-link.
	 *
	 * @param accountLink The account link to remove.
	 */
	public void removeOutlink(final AccountLink accountLink) {
		this.historicalOutlinks.remove(accountLink.getHeight(), accountLink.getOtherAccountAddress(), accountLink.getAmount());
	}

	@Override
	public Iterator<AccountLink> getOutlinksIterator(final BlockHeight startHeight, final BlockHeight endHeight) {
		return this.historicalOutlinks.outlinksIterator(startHeight, endHeight);
	}

	@Override
	public int getOutlinksSize(final BlockHeight blockHeight) {
		return this.historicalOutlinks.outlinksSize(blockHeight);
	}

	/**
	 * Removes all historical outlinks that are older than the specified height.
	 *
	 * @param minHeight The minimum height of outlinks to keep.
	 */
	public void prune(final BlockHeight minHeight) {
		this.historicalOutlinks.prune(minHeight);
	}

	/**
	 * Sets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @param importance The importance.
	 */
	public void setImportance(final BlockHeight blockHeight, final double importance) {
		this.importanceHeight = blockHeight;
		this.importance = importance;
	}

	@Override
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

	@Override
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

	@Override
	public BlockHeight getHeight() {
		return this.importanceHeight;
	}

	@Override
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
