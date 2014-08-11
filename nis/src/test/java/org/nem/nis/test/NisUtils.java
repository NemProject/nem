package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.secret.AccountLink;

import java.util.*;

/**
 * Static class containing NIS test helper functions.
 */
public class NisUtils {

	/**
	 * Creates a DB Block that can be mapped to a model Block.
	 *
	 * @param timeStamp The block timestamp.
	 * @return The db block.
	 */
	public static org.nem.nis.dbmodel.Block createDbBlockWithTimeStamp(final int timeStamp) {
		final org.nem.nis.dbmodel.Account account = new org.nem.nis.dbmodel.Account();
		account.setPublicKey(Utils.generateRandomPublicKey());

		final org.nem.nis.dbmodel.Block block = new org.nem.nis.dbmodel.Block();
		block.setForgerId(account);
		block.setTimestamp(timeStamp);
		block.setHeight(10L);
		block.setForgerProof(Utils.generateRandomBytes(64));
		block.setBlockTransfers(new ArrayList<>());
		return block;
	}

	/**
	 * Creates a new random Block.
	 */
	public static Block createRandomBlock() {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				TimeInstant.ZERO,
				BlockHeight.ONE);
	}

	/**
	 * Creates a new random Block with the specified height.
	 */
	public static Block createRandomBlockWithHeight(long height) {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				TimeInstant.ZERO,
				new BlockHeight(height));
	}

	/**
	 * Creates a new random Block with the specified timestamp.
	 */
	public static Block createRandomBlockWithTimeStamp(int timestamp) {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				new TimeInstant(timestamp),
				BlockHeight.ONE);
	}

	/**
	 * Creates a hashes list.
	 *
	 * @param numHashes The number of hashes desired.
	 * @return A hashes list.
	 */
	public static List<Hash> createHashesList(final int numHashes) {
		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < numHashes; ++i)
			hashes.add(Utils.generateRandomHash());

		return hashes;
	}

	/**
	 * Creates a time provider that returns the specified instants.
	 *
	 * @param rawInstants The raw instant values.
	 * @return The time provider.
	 */
	public static TimeProvider createMockTimeProvider(final int... rawInstants) {
		final TimeInstant[] instants = new TimeInstant[rawInstants.length - 1];
		for (int i = 1; i < rawInstants.length; ++i)
			instants[i - 1] = new TimeInstant(rawInstants[i]);

		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(rawInstants[0]), instants);
		return timeProvider;
	}

	/**
	 * Creates a new account link.
	 *
	 * @param blockHeight The block height.
	 * @param amount The amount.
	 * @param address The address.
	 * @return The account link.
	 */
	public static AccountLink createLink(final int blockHeight, final long amount, final String address) {
		return new AccountLink(
				new BlockHeight(blockHeight),
				Amount.fromNem(amount),
				Address.fromEncoded(address));
	}
}
