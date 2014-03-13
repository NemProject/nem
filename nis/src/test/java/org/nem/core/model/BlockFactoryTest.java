package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class BlockFactoryTest {
    @Test(expected = InvalidParameterException.class)
    public void cannotDeserializeUnknownBlock() {
        // Arrange:
        final JSONObject object = new JSONObject();
        object.put("type", 7);
        final JsonDeserializer deserializer = new JsonDeserializer(object, null);

        // Act:
        BlockFactory.VERIFIABLE.deserialize(deserializer);
    }

    @Test
    public void canDeserializeVerifiableBlock() {
        // Arrange:
        final Account forger = Utils.generateRandomAccount();
        final Block originalBlock = new Block(forger, Utils.generateRandomBytes(), 0, 1);
        final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalBlock, new MockAccountLookup());

        // Act:
        final Block block = BlockFactory.VERIFIABLE.deserialize(deserializer);

        // Assert:
        Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getSignature(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void canDeserializeNonVerifiableBlock() {
        // Arrange:
        final Account forger = Utils.generateRandomAccount();
        final Block originalBlock = new Block(forger, Utils.generateRandomBytes(), 0, 1);
        final Deserializer deserializer = Utils.roundtripSerializableEntity(
            originalBlock.asNonVerifiable(),
            new MockAccountLookup());

        // Act:
        final Block block = BlockFactory.NON_VERIFIABLE.deserialize(deserializer);

        // Assert:
        Assert.assertThat(block, IsInstanceOf.instanceOf(Block.class));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getSignature(), IsEqual.equalTo(null));
    }
}
