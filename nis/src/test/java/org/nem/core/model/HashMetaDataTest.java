package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

import java.util.HashMap;

public class HashMetaDataTest {

	@Test
	public void canCreateHashMetaData() {
		// Act:
		final HashMetaData meta = new HashMetaData(new BlockHeight(10), new TimeInstant(123));

		// Assert:
		Assert.assertThat(meta.getHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(meta.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final HashMap<String, HashMetaData> map = createTestObjects();

		// Assert:
		Assert.assertThat(map.get("copy"), IsEqual.equalTo(map.get("original")));
		Assert.assertThat(map.get("differentHeight"), IsNot.not(IsEqual.equalTo(map.get("original"))));
		Assert.assertThat(map.get("differentTimeStamp"), IsNot.not(IsEqual.equalTo(map.get("original"))));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final HashMap<String, HashMetaData> map = createTestObjects();

		// Assert:
		Assert.assertThat(map.get("copy").hashCode(), IsEqual.equalTo(map.get("original").hashCode()));
		Assert.assertThat(map.get("differentHeight").hashCode(), IsNot.not(IsEqual.equalTo(map.get("original").hashCode())));
		Assert.assertThat(map.get("differentTimeStamp").hashCode(), IsNot.not(IsEqual.equalTo(map.get("original").hashCode())));
	}

	private static HashMap<String, HashMetaData> createTestObjects() {
		return new HashMap<String, HashMetaData>() {
			{
				this.put("original", new HashMetaData(new BlockHeight(10), new TimeInstant(123)));
				this.put("copy", new HashMetaData(new BlockHeight(10), new TimeInstant(123)));
				this.put("differentHeight", new HashMetaData(new BlockHeight(11), new TimeInstant(123)));
				this.put("differentTimeStamp", new HashMetaData(new BlockHeight(10), new TimeInstant(234)));
			}
		};
	}

	// endregion
}
