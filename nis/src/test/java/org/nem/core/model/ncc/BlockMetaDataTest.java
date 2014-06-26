package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;

public class BlockMetaDataTest {

	@Test
	public void canCreateBlockMetaData() {
		// Act:
		final BlockMetaData metaData = new BlockMetaData(new Hash(new byte[]{ 1,2,3,4 }));

		// Assert:
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(new Hash(new byte[]{ 1,2,3,4 })));
	}

	@Test
	public void canRoundtripBlockMetaData() {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new BlockMetaData(new Hash(new byte[]{ 1,2,3,4 })),
				null);
		final BlockMetaData metaData = new BlockMetaData(deserializer);

		// Assert:
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(new Hash(new byte[]{ 1,2,3,4 })));
	}
}