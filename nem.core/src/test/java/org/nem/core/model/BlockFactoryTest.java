package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class BlockFactoryTest {

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownBlock() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		BlockFactory.VERIFIABLE.deserialize(deserializer);
	}

	@Test
	public void canDeserializeNemesisVerifiableBlock() {
		// Assert:
		canDeserializeVerifiableBlock(createNemesisBlock(), -1);
	}

	@Test
	public void canDeserializeNemesisNonVerifiableBlock() {
		// Assert:
		canDeserializeNonVerifiableBlock(createNemesisBlock(), -1);
	}

	@Test
	public void canDeserializeRegularVerifiableBlock() {
		// Assert:
		canDeserializeVerifiableBlock(createRegularBlock(), 1);
	}

	@Test
	public void canDeserializeRegularNonVerifiableBlock() {
		// Assert:
		canDeserializeNonVerifiableBlock(createRegularBlock(), 1);
	}

	private static void canDeserializeVerifiableBlock(final Block originalBlock, final int expectedType) {
		// Arrange:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalBlock, new MockAccountLookup());

		// Act:
		final Block block = BlockFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
		Assert.assertThat(block.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(block.getSignature(), IsNull.notNullValue());
	}

	private static void canDeserializeNonVerifiableBlock(final Block originalBlock, final int expectedType) {
		// Arrange:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				originalBlock.asNonVerifiable(),
				new MockAccountLookup());

		// Act:
		final Block block = BlockFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
		Assert.assertThat(block.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(block.getSignature(), IsNull.nullValue());
	}

	private static Block createNemesisBlock() {
		return new Block(Utils.generateRandomAccount(), Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
	}

	private static Block createRegularBlock() {
		return new Block(Utils.generateRandomAccount(), Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(2));
	}
}
