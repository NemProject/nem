package org.nem.core.crypto;

import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class HashChainTest {

	@Test
	public void hashChainCanBeCreatedAroundRawHashes() {
		// Arrange:
		final List<Hash> hashes = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash());

		final List<byte[]> rawHashes = hashes.stream().map(Hash::getRaw).collect(Collectors.toList());

		// Act:
		final HashChain hashChain1 = HashChain.fromRawHashes(rawHashes);
		final HashChain hashChain2 = new HashChain(hashes);

		// Assert:
		MatcherAssert.assertThat(hashChain1.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(hashChain2.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(hashChain1, IsEqual.equalTo(hashChain2));
	}

	@Test
	public void hashChainCanBeRoundTripped() {
		// Arrange:
		final List<Hash> hashes = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash());

		final HashChain hashChain1 = new HashChain(hashes);

		// Act:
		final HashChain hashChain2 = this.createRoundTrippedHashChain(hashChain1);

		// Assert:
		MatcherAssert.assertThat(hashChain1.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(hashChain2.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(hashChain1, IsEqual.equalTo(hashChain2));
	}

	private HashChain createRoundTrippedHashChain(final HashChain originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction, null);
		return new HashChain(deserializer);
	}
}
