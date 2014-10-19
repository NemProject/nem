package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
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
	public void canDeserializeNemesisBlock() {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(new MockAccountLookup());

		final JSONObject object = new JSONObject();
		object.put("type", -1);
		final JsonDeserializer deserializer = new JsonDeserializer(object, context);

		// Act:
		final Block block = BlockFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(block, IsInstanceOf.instanceOf(NemesisBlock.class));
		Assert.assertThat(
				HashUtils.calculateHash(block),
				IsEqual.equalTo(HashUtils.calculateHash(NemesisBlock.fromResource(context))));
	}

	@Test
	public void canDeserializeRegularVerifiableBlock() {
		// Arrange:
		final Account forger = Utils.generateRandomAccount();
		final Block originalBlock = new Block(forger, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalBlock, new MockAccountLookup());

		// Act:
		final Block block = BlockFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void canDeserializeRegularNonVerifiableBlock() {
		// Arrange:
		final Account forger = Utils.generateRandomAccount();
		final Block originalBlock = new Block(forger, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				originalBlock.asNonVerifiable(),
				new MockAccountLookup());

		// Act:
		final Block block = BlockFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getSignature(), IsNull.nullValue());
	}
}
