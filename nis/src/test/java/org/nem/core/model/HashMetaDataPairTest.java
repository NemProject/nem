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
		Assert.assertThat(pair.getMetaData(), IsEqual.equalTo(metaData));
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

	private static HashMap<String, HashMetaDataPair> createTestObjects() {
		final Hash hash = Utils.generateRandomHash();
		final HashMetaData metaData = new HashMetaData(new BlockHeight(10), new TimeInstant(123));
		return new HashMap<String, HashMetaDataPair>() {
			{
				this.put("original", new HashMetaDataPair(hash, metaData));
				this.put("copy", new HashMetaDataPair(hash, metaData));
				this.put("differentHash", new HashMetaDataPair(Utils.generateRandomHash(), metaData));
				this.put("differentMetaData", new HashMetaDataPair(hash, new HashMetaData(new BlockHeight(10), new TimeInstant(234))));
			}
		};
	}

	// endregion
}
