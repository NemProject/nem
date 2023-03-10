package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.NemProperties;
import org.nem.core.model.NetworkInfos;
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

	private final BlockHeight multisigMOfNForkHeight;
	private final BlockHeight mosaicsForkHeight;
	private final BlockHeight feeForkHeight;
	private final BlockHeight remoteAccountForkHeight;
	private final BlockHeight mosaicRedefinitionForkHeight;
	private final BlockHeight secondFeeForkHeight;

	private ForkConfiguration(final Builder builder) {
		this.treasuryReissuanceForkHeight = builder.treasuryReissuanceForkHeight;
		this.treasuryReissuanceForkTransactionHashes = builder.treasuryReissuanceForkTransactionHashes;
		this.treasuryReissuanceForkFallbackTransactionHashes = builder.treasuryReissuanceForkFallbackTransactionHashes;
		this.multisigMOfNForkHeight = builder.multisigMOfNForkHeight;
		this.mosaicsForkHeight = builder.mosaicsForkHeight;
		this.feeForkHeight = builder.feeForkHeight;
		this.remoteAccountForkHeight = builder.remoteAccountForkHeight;
		this.mosaicRedefinitionForkHeight = builder.mosaicRedefinitionForkHeight;
		this.secondFeeForkHeight = builder.secondFeeForkHeight;
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

	/**
	 * Gets the height of the fork at which to enable multisig M-of-N.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getmultisigMOfNForkHeight() {
		return this.multisigMOfNForkHeight;
	}

	/**
	 * Gets the height of the mosaics fork.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getMosaicsForkHeight() {
		return this.mosaicsForkHeight;
	}

	/**
	 * Gets the height of the fee fork.
	 *
	 * @return The height of the fork.
	 */

	public BlockHeight getFeeForkHeight() {
		return this.feeForkHeight;
	}

	/**
	 * Gets the height of the remote account fork.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getRemoteAccountForkHeight() {
		return this.remoteAccountForkHeight;
	}

	/**
	 * Gets the height of the mosaic redefinition fork.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getMosaicRedefinitionForkHeight() {
		return this.mosaicRedefinitionForkHeight;
	}

	/**
	 * Gets the height of the second fee fork.
	 *
	 * @return The height of the fork.
	 */
	public BlockHeight getSecondFeeForkHeight() {
		return this.secondFeeForkHeight;
	}

	/**
	 * Creates a builder for a fork configuration.
	 */
	public static class Builder {
		private BlockHeight treasuryReissuanceForkHeight;
		private List<Hash> treasuryReissuanceForkTransactionHashes;
		private List<Hash> treasuryReissuanceForkFallbackTransactionHashes;
		private BlockHeight multisigMOfNForkHeight;
		private BlockHeight mosaicsForkHeight;
		private BlockHeight feeForkHeight;
		private BlockHeight remoteAccountForkHeight;
		private BlockHeight mosaicRedefinitionForkHeight;
		private BlockHeight secondFeeForkHeight;

		/**
		 * Creates the default builder.
		 */
		public Builder() {
			final int version = NetworkInfos.getDefault().getVersion() << 24;
			treasuryReissuanceForkHeight = new BlockHeight(1);
			treasuryReissuanceForkTransactionHashes = Collections.emptyList();
			treasuryReissuanceForkFallbackTransactionHashes = Collections.emptyList();
			multisigMOfNForkHeight = new BlockHeight(BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version));
			mosaicsForkHeight = new BlockHeight(BlockMarkerConstants.MOSAICS_FORK(version));
			feeForkHeight = new BlockHeight(BlockMarkerConstants.FEE_FORK(version));
			remoteAccountForkHeight = new BlockHeight(BlockMarkerConstants.REMOTE_ACCOUNT_FORK(version));
			mosaicRedefinitionForkHeight = new BlockHeight(BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(version));
			secondFeeForkHeight = new BlockHeight(BlockMarkerConstants.SECOND_FEE_FORK(version));
		}

		/**
		 * Creates a builder for a fork configuration from nis configuration.
		 *
		 * @param properties nis properties.
		 */
		public Builder(final NemProperties properties) {
			final int version = NetworkInfos.getDefault().getVersion() << 24;
			this.treasuryReissuanceForkHeight = new BlockHeight(properties.getOptionalInteger("nis.treasuryReissuanceForkHeight", 1));
			this.treasuryReissuanceForkTransactionHashes = Collections
					.unmodifiableList(parseHashes(properties, "nis.treasuryReissuanceForkTransactionHashes"));
			this.treasuryReissuanceForkFallbackTransactionHashes = Collections
					.unmodifiableList(parseHashes(properties, "nis.treasuryReissuanceForkFallbackTransactionHashes"));

			this.multisigMOfNForkHeight = new BlockHeight(properties.getOptionalLong("nis.multisigMOfNForkHeight",
					BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version)));
			this.mosaicsForkHeight = new BlockHeight(properties.getOptionalLong("nis.mosaicsForkHeight",
					BlockMarkerConstants.MOSAICS_FORK(version)));
			this.feeForkHeight = new BlockHeight(properties.getOptionalLong("nis.feeForkHeight",
					BlockMarkerConstants.FEE_FORK(version)));
			this.remoteAccountForkHeight = new BlockHeight(properties.getOptionalLong("nis.remoteAccountForkHeight",
					BlockMarkerConstants.REMOTE_ACCOUNT_FORK(version)));
			this.mosaicRedefinitionForkHeight = new BlockHeight(properties.getOptionalLong("nis.mosaicRedefinitionForkHeight",
					BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(version)));
			this.secondFeeForkHeight = new BlockHeight(properties.getOptionalLong("nis.secondFeeForkHeight",
					BlockMarkerConstants.SECOND_FEE_FORK(version)));
		}

		private static List<Hash> parseHashes(final NemProperties properties, final String propertyName) {
			return Arrays.stream(properties.getOptionalStringArray(propertyName, "")).map(s -> Hash.fromHexString(s.trim()))
					.collect(Collectors.toList());
		}

		/**
		 * Sets the treasury reissuance fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder treasuryReissuanceForkHeight(final BlockHeight treasuryReissuanceForkHeight) {
			this.treasuryReissuanceForkHeight = treasuryReissuanceForkHeight;
			return this;
		}

		/**
		 * Sets the treasury reissuance fork transaction hashes
		 *
		 * @return The fork configuration builder.
		 */
		public Builder treasuryReissuanceForkTransactionHashes(final List<Hash> treasuryReissuanceForkTransactionHashes) {
			this.treasuryReissuanceForkTransactionHashes = treasuryReissuanceForkTransactionHashes;
			return this;
		}

		/**
		 * Sets the treasury reissuance fork fallback transaction hashes
		 *
		 * @return The fork configuration builder.
		 */
		public Builder treasuryReissuanceForkFallbackTransactionHashes(
				final List<Hash> treasuryReissuanceForkFallbackTransactionHashes) {
			this.treasuryReissuanceForkFallbackTransactionHashes = treasuryReissuanceForkFallbackTransactionHashes;
			return this;
		}

		/**
		 * Sets the multisig m-of-n fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder multisigMOfNForkHeight(final BlockHeight multisigMOfNForkHeight) {
			this.multisigMOfNForkHeight = multisigMOfNForkHeight;
			return this;
		}

		/**
		 * Sets the mosaics fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder mosaicsForkHeight(final BlockHeight mosaicsForkHeight) {
			this.mosaicsForkHeight = mosaicsForkHeight;
			return this;
		}

		/**
		 * Sets the fee fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder feeForkHeight(final BlockHeight feeForkHeight) {
			this.feeForkHeight = feeForkHeight;
			return this;
		}

		/**
		 * Sets the remote account fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder remoteAccountForkHeight(final BlockHeight remoteAccountForkHeight) {
			this.remoteAccountForkHeight = remoteAccountForkHeight;
			return this;
		}

		/**
		 * Sets the mosaic redefinition fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder mosaicRedefinitionForkHeight(final BlockHeight mosaicRedefinitionForkHeight) {
			this.mosaicRedefinitionForkHeight = mosaicRedefinitionForkHeight;
			return this;
		}

		/**
		 * Sets the second fee fork height
		 *
		 * @return The fork configuration builder.
		 */
		public Builder secondFeeForkHeight(final BlockHeight secondFeeForkHeight) {
			this.secondFeeForkHeight = secondFeeForkHeight;
			return this;
		}

		/**
		 * Builds the fork configuration.
		 *
		 * @return The fork configuration.
		 */
		public ForkConfiguration build() {
			return new ForkConfiguration(this);
		}
	}
}
