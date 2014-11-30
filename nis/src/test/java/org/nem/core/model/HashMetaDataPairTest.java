package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

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
}
