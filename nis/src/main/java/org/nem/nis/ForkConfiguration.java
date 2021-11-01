package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.NemProperties;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that encapsulates fork configuration data.
 */
public class ForkConfiguration {
	private final BlockHeight treasuryReissuanceForkHeight;
	private final List<Hash> treasuryReissuanceForkTransactionHashes;
	private final List<Hash> treasuryReissuanceForkFallbackTransactionHashes;

	/**
	 * Creates a new default configuration object.
	 */
	public ForkConfiguration() {
		this(new BlockHeight(1), new ArrayList<Hash>(), new ArrayList<Hash>());
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param treasuryReissuanceForkHeight The height of the fork at which to reissue the treasury.
	 * @param treasuryReissuanceForkTransactionHashes The hashes of transactions that are allowed in the treasury reissuance fork block
	 *            (preferred).
	 * @param treasuryReissuanceForkFallbackTransactionHashes The hashes of transactions that are allowed in the treasury reissuance fork
	 *            block (fallback).
	 */
	public ForkConfiguration(final BlockHeight treasuryReissuanceForkHeight, final List<Hash> treasuryReissuanceForkTransactionHashes,
			final List<Hash> treasuryReissuanceForkFallbackTransactionHashes) {
		this.treasuryReissuanceForkHeight = treasuryReissuanceForkHeight;
		this.treasuryReissuanceForkTransactionHashes = Collections.unmodifiableList(treasuryReissuanceForkTransactionHashes);
		this.treasuryReissuanceForkFallbackTransactionHashes = Collections
				.unmodifiableList(treasuryReissuanceForkFallbackTransactionHashes);
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public ForkConfiguration(final NemProperties properties) {
		this.treasuryReissuanceForkHeight = new BlockHeight(properties.getOptionalInteger("nis.treasuryReissuanceForkHeight", 1));
		this.treasuryReissuanceForkTransactionHashes = Collections
				.unmodifiableList(ForkConfiguration.parseHashes(properties, "nis.treasuryReissuanceForkTransactionHashes"));
		this.treasuryReissuanceForkFallbackTransactionHashes = Collections
				.unmodifiableList(ForkConfiguration.parseHashes(properties, "nis.treasuryReissuanceForkFallbackTransactionHashes"));
	}

	private static List<Hash> parseHashes(final NemProperties properties, final String propertyName) {
		return Arrays.stream(properties.getOptionalStringArray(propertyName, "")).map(s -> Hash.fromHexString(s.trim()))
				.collect(Collectors.toList());
	}

	/**
	 * Gets the height of the fork at which to reissue the treasury.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getTreasuryReissuanceForkHeight() {
		return this.treasuryReissuanceForkHeight;
	}

	/**
	 * Gets the hashes of transactions that are allowed in the treasury reissuance fork block (preferred).
	 *
	 * @return The hashes.
	 */
	public Collection<Hash> getTreasuryReissuanceForkTransactionHashes() {
		return this.treasuryReissuanceForkTransactionHashes;
	}

	/**
	 * Gets the hashes of transactions that are allowed in the treasury reissuance fork block (fallback).
	 *
	 * @return The hashes.
	 */
	public Collection<Hash> getTreasuryReissuanceForkFallbackTransactionHashes() {
		return this.treasuryReissuanceForkFallbackTransactionHashes;
	}
}
