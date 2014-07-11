package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.serialization.*;
import org.nem.peer.node.*;

public class LocalNodeDeserializerTest {

	@Test
	public void localNodeDeserializerCanLoadNodeWithOwnedIdentity() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final NodeMetaData metaData = new NodeMetaData("p", "a", NodeVersion.ZERO);

		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeObject("identity", childSerializer -> {
			childSerializer.writeBigInteger("private-key", keyPair.getPrivateKey().getRaw());
			childSerializer.writeString("name", "trudy");
		});
		serializer.writeObject("endpoint", endpoint);
		serializer.writeObject("metaData", metaData);

		// Act:
		final Deserializer deserializer = new JsonDeserializer(serializer.getObject(), null);
		final Node node = new LocalNodeDeserializer().deserialize(deserializer);

		// Assert:
		Assert.assertThat(node.getIdentity(), IsEqual.equalTo(identity));
		Assert.assertThat(node.getIdentity().getKeyPair().getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		Assert.assertThat(node.getIdentity().isOwned(), IsEqual.equalTo(true));
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(endpoint));
		Assert.assertThat(node.getMetaData().getPlatform(), IsEqual.equalTo("p"));
	}
}