package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class BlockHeightTest {

	// region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		MatcherAssert.assertThat(BlockHeight.ONE, IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(BlockHeight.MAX, IsEqual.equalTo(new BlockHeight(Long.MAX_VALUE)));
	}

	// endregion

	// region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeHeight() {
		// Act:
		new BlockHeight(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundZeroHeight() {
		// Act:
		new BlockHeight(0);
	}

	@Test
	public void canBeCreatedAroundPositiveHeight() {
		// Act:
		final BlockHeight height = new BlockHeight(1);

		// Assert:
		MatcherAssert.assertThat(height.getRaw(), IsEqual.equalTo(1L));
	}

	// endregion

	// region next / prev

	@Test
	public void nextHeightIsOneGreaterThanCurrentHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(45);

		// Act:
		final BlockHeight nextHeight = height.next();

		// Assert:
		MatcherAssert.assertThat(nextHeight, IsNot.not(IsEqual.equalTo(height)));
		MatcherAssert.assertThat(nextHeight, IsEqual.equalTo(new BlockHeight(46)));
	}

	@Test
	public void prevHeightIsOneLessThanCurrentHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(45);

		// Act:
		final BlockHeight nextHeight = height.prev();

		// Assert:
		MatcherAssert.assertThat(nextHeight, IsNot.not(IsEqual.equalTo(height)));
		MatcherAssert.assertThat(nextHeight, IsEqual.equalTo(new BlockHeight(44)));
	}

	// endregion

	// region subtract

	@Test
	public void heightsCanBeSubtracted() {
		// Arrange:
		final BlockHeight height1 = new BlockHeight(17);
		final BlockHeight height2 = new BlockHeight(3);

		// Assert:
		MatcherAssert.assertThat(14L, IsEqual.equalTo(height1.subtract(height2)));
		MatcherAssert.assertThat(-14L, IsEqual.equalTo(height2.subtract(height1)));
	}

	// endregion

	// region serialization

	@Test
	public void heightCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockHeight height = new BlockHeight(142);

		// Act:
		height.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		MatcherAssert.assertThat(jsonObject.get("height"), IsEqual.equalTo(142L));
	}

	@Test
	public void responseCanBeRoundTripped() {
		// Act:
		final BlockHeight height = createRoundTrippedHeight(new BlockHeight(142));

		// Assert:
		MatcherAssert.assertThat(height.getRaw(), IsEqual.equalTo(142L));
	}

	private static BlockHeight createRoundTrippedHeight(final BlockHeight originalHeight) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalHeight, null);
		return new BlockHeight(deserializer);
	}

	// endregion

	// region inline serialization

	@Test
	public void canWriteBlockHeight() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockHeight height = new BlockHeight(0x8712411223456L);

		// Act:
		BlockHeight.writeTo(serializer, "Height", height);

		// Assert:
		final JSONObject object = serializer.getObject();
		MatcherAssert.assertThat(object.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(object.get("Height"), IsEqual.equalTo(0x8712411223456L));
	}

	@Test
	public void canRoundtripBlockHeight() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockHeight originalHeight = new BlockHeight(0x8712411223456L);

		// Act:
		BlockHeight.writeTo(serializer, "Height", originalHeight);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final BlockHeight height = BlockHeight.readFrom(deserializer, "Height");

		// Assert:
		MatcherAssert.assertThat(height, IsEqual.equalTo(originalHeight));
	}

	// endregion
}
