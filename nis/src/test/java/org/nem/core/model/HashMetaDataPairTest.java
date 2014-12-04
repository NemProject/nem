package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.util.HashMap;

public class HashMetaDataPairTest {

	@Test
	public void canCreateHashMetaDataPair() {
		// Act:
		final Hash hash = Utils.generateRandomHash();
		final HashMetaData metaData = new HashMetaData(new BlockHeight(12), new TimeInstant(123));
		final HashMetaDataPair pair = new HashMetaDataPair(hash, metaData);

		// Assert:
		Assert.assertThat(pair.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(12)));
		Assert.assertThat(metaData.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final HashMap<String, HashMetaDataPair> map = createTestObjects();

		// Assert:
		Assert.assertThat(map.get("copy"), IsEqual.equalTo(map.get("original")));
		Assert.assertThat(map.get("differentHash"), IsNot.not(IsEqual.equalTo(map.get("original"))));
		Assert.assertThat(map.get("differentMetaData"), IsNot.not(IsEqual.equalTo(map.get("original"))));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final HashMap<String, HashMetaDataPair> map = createTestObjects();

		// Assert:
		Assert.assertThat(map.get("copy").hashCode(), IsEqual.equalTo(map.get("original").hashCode()));
		Assert.assertThat(map.get("differentHash").hashCode(), IsNot.not(IsEqual.equalTo(map.get("original").hashCode())));
		Assert.assertThat(map.get("differentMetaData").hashCode(), IsNot.not(IsEqual.equalTo(map.get("original").hashCode())));
	}

	private HashMap<String, HashMetaDataPair> createTestObjects() {
		final Hash hash = Utils.generateRandomHash();
		final HashMetaData metaData = new HashMetaData(new BlockHeight(10), new TimeInstant(123));
		HashMap<String, HashMetaDataPair> map = new HashMap<>();
		map.put("original", new HashMetaDataPair(hash, metaData));
		map.put("copy", new HashMetaDataPair(hash, metaData));
		map.put("differentHash", new HashMetaDataPair(Utils.generateRandomHash(), metaData));
		map.put("differentMetaData",  new HashMetaDataPair(hash, new HashMetaData(new BlockHeight(10), new TimeInstant(234))));

		return map;
	}

	// endregion
}
