package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class HashChainTest {

	@Test
	public void hashChainCanBeCreatedAroundRawHashes() {
		// Arrange:
		final List<Hash> hashes = Arrays.asList(
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash());

		final List<byte[]> rawHashes = hashes.stream().map(Hash::getRaw).collect(Collectors.toList());

		// Act:
		final HashChain hashChain1 = HashChain.fromRawHashes(rawHashes);
		final HashChain hashChain2 = new HashChain(hashes);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(3));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(3));
		Assert.assertThat(hashChain1, IsEqual.equalTo(hashChain2));
	}

	@Test
	public void hashChainCanBeRoundTripped() {
		// Arrange:
		final List<Hash> hashes = Arrays.asList(
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash());

		final HashChain hashChain1 = new HashChain(hashes);

		// Act:
		final HashChain hashChain2 = this.createRoundTrippedHashChain(hashChain1);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(3));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(3));
		Assert.assertThat(hashChain1, IsEqual.equalTo(hashChain2));
	}

	private HashChain createRoundTrippedHashChain(final HashChain originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction, null);
		return new HashChain(deserializer);
	}
}
