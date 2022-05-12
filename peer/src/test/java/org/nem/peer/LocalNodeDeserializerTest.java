package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class LocalNodeDeserializerTest {

	@Test
	public void canDeserializeNodeIdentityWithPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		final TestDeserializerBuilder builder = new TestDeserializerBuilder();
		builder.writeIdentity("trudy", keyPair);
		builder.writeEndpoint();
		builder.writeMetaData();

		// Act:
		final Node node = new LocalNodeDeserializer().deserialize(builder.getDeserializer());

		// Assert:
		MatcherAssert.assertThat(node.getIdentity(), IsEqual.equalTo(identity));
		MatcherAssert.assertThat(node.getIdentity().getKeyPair().getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(node.getIdentity().getName(), IsEqual.equalTo("trudy"));
		MatcherAssert.assertThat(node.getIdentity().isOwned(), IsEqual.equalTo(true));
	}

	@Test
	public void canDeserializeNodeEndpoint() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);

		final TestDeserializerBuilder builder = new TestDeserializerBuilder();
		builder.writeIdentity();
		builder.writeEndpoint(endpoint);
		builder.writeMetaData();

		// Act:
		final Node node = new LocalNodeDeserializer().deserialize(builder.getDeserializer());

		// Assert:
		MatcherAssert.assertThat(node.getEndpoint(), IsEqual.equalTo(endpoint));
	}

	@Test
	public void canDeserializeNodeMetaDataApplication() {
		// Assert:
		assertRoundTripOfMetaDataApplication("app");
	}

	@Test
	public void canDeserializeNodeMetaDataWithoutApplication() {
		// Assert:
		assertRoundTripOfMetaDataApplication(null);
	}

	private static void assertRoundTripOfMetaDataApplication(final String application) {
		// Arrange:
		final TestDeserializerBuilder builder = new TestDeserializerBuilder();
		builder.writeIdentity();
		builder.writeEndpoint();
		builder.writeMetaDataApplicationOnly(application);

		// Act:
		final Node node = new LocalNodeDeserializer().deserialize(builder.getDeserializer());

		// Assert:
		MatcherAssert.assertThat(node.getMetaData(), IsEqual.equalTo(new NodeMetaData(null, application)));
	}

	private static class TestDeserializerBuilder {
		private final JsonSerializer serializer = new JsonSerializer(true);

		private void writeIdentity() {
			this.writeIdentity("bob", new KeyPair());
		}

		private void writeIdentity(final String name, final KeyPair keyPair) {
			this.serializer.writeObject("identity", childSerializer -> {
				childSerializer.writeBigInteger("private-key", keyPair.getPrivateKey().getRaw());
				childSerializer.writeString("name", name);
			});
		}

		private void writeEndpoint() {
			this.writeEndpoint(new NodeEndpoint("http", "localhost", 8080));
		}

		private void writeEndpoint(final NodeEndpoint endpoint) {
			this.serializer.writeObject("endpoint", endpoint);
		}

		private void writeMetaData() {
			this.writeMetaDataApplicationOnly("app");
		}

		private void writeMetaDataApplicationOnly(final String application) {
			this.serializer.writeObject("metaData", childSerializer -> childSerializer.writeString("application", application));
		}

		private Deserializer getDeserializer() {
			return Utils.createDeserializer(this.serializer.getObject());
		}
	}
}
