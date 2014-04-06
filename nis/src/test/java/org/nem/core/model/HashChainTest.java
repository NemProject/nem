package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashChainTest {
	public static final List<byte[]> dummyList = new ArrayList<>(
			Arrays.asList(
					new byte[]{1,2,3},
					new byte[]{4,5,6,7},
					new byte[]{8,9}
			)
	);

	//region Constructors

	@Test
	public void ctorHaveNoImpactOnSize() {
		// Arrange:
		final HashChain hashChain = new HashChain(100);

		// Assert:
		Assert.assertThat(hashChain.size(), IsEqual.equalTo(0));
	}

	@Test
	public void ctorWithListChangesSize() {
		// Arrange:
		final HashChain hashChain = new HashChain(dummyList);

		// Assert:
		Assert.assertThat(hashChain.size(), IsEqual.equalTo(dummyList.size()));
	}
	// endregion


	// region Add
	@Test
	public void addChangesSize() {
		// Arrange:
		final HashChain hashChain = new HashChain(100);

		// Act:
		hashChain.add(null);
		hashChain.add(null);
		hashChain.add(null);

		// Assert:
		Assert.assertThat(hashChain.size(), IsEqual.equalTo(3));
	}
	// endregion

	// region FindFirst simple
	@Test
	public void canCompareEmptyChains() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(10);
		final HashChain hashChain2 = new HashChain(20);

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(i, IsEqual.equalTo(0));
	}

	@Test
	public void canCompareWithSelf() {
		// Arrange:
		final HashChain hashChain = new HashChain(dummyList);

		// Act:
		int i = hashChain.findFirstDifferen(hashChain);

		// Assert:
		Assert.assertThat(hashChain.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(i, IsEqual.equalTo(dummyList.size()));
	}

	@Test
	public void canCompareWithSelfUsingAdd() {
		// Arrange:
		final HashChain hashChain = new HashChain(0);
		for (byte[] elem : dummyList) {
			hashChain.add(elem);
		}

		// Act:
		int i = hashChain.findFirstDifferen(hashChain);

		// Assert:
		Assert.assertThat(hashChain.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(i, IsEqual.equalTo(dummyList.size()));
	}

	// endregion

	// region FindFirst two chains

	@Test
	public void canCompareTwoChains() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(dummyList);
		final HashChain hashChain2 = new HashChain(0);
		for (byte[] elem : dummyList) {
			// deep copy
			hashChain2.add(elem.clone());
		}

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(i, IsEqual.equalTo(dummyList.size()));
	}


	@Test
	public void canCompareChainsWithDifferentLengths() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(dummyList);
		final HashChain hashChain2 = new HashChain(dummyList);

		hashChain2.add(new byte[]{1,2,3,4});
		hashChain2.add(new byte[]{5,6,7,8});

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(dummyList.size() + 2));
		Assert.assertThat(i, IsEqual.equalTo(dummyList.size()));
	}

	// not that this behaviour is actually needed anywhere
	@Test
	public void canCompareListWithNulls() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(0);
		final HashChain hashChain2 = new HashChain(0);
		hashChain1.add(null);
		hashChain1.add(new byte[]{1,2,3});
		hashChain2.add(null);
		hashChain2.add(new byte[]{1,2,3});

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(2));
		Assert.assertThat(i, IsEqual.equalTo(2));
	}

	@Test
	public void findsDifferentElem() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(dummyList);
		final HashChain hashChain2 = new HashChain(dummyList);

		hashChain1.add(new byte[]{1,2});
		hashChain2.add(new byte[]{2,1});

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(dummyList.size() + 1));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(dummyList.size() + 1));
		Assert.assertThat(i, IsEqual.equalTo(3));
	}
	// endregion

	@Test
	public void hashchainCanBeRoundTripped() {
		// Arrange:
		final HashChain hashChain1 = new HashChain(dummyList);
		final HashChain hashChain2 = createRoundTrippedHashChain(hashChain1);

		// Act:
		int i = hashChain1.findFirstDifferen(hashChain2);

		// Assert:
		Assert.assertThat(hashChain1.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(hashChain2.size(), IsEqual.equalTo(dummyList.size()));
		Assert.assertThat(i, IsEqual.equalTo(dummyList.size()));
	}

	private HashChain createRoundTrippedHashChain(HashChain originalTransaction) {
		// Act:
		Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction, null);
		return new HashChain(deserializer);
	}
}
