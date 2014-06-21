package org.nem.deploy;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.utils.StringEncoder;
import org.springframework.http.MediaType;

import java.io.*;

public class JsonSerializationPolicyTest {

	//region getMediaType

	@Test
	public void policySupportsApplicationJsonMediaType() {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);

		// Act:
		final MediaType mediaType = policy.getMediaType();

		// Assert:
		Assert.assertThat(mediaType.getType(), IsEqual.equalTo("application"));
		Assert.assertThat(mediaType.getSubtype(), IsEqual.equalTo("json"));
		Assert.assertThat(mediaType.getCharSet(), IsNull.nullValue());
	}

	//endregion

	//region toBytes

	@Test
	public void toBytesCreatesJsonStringWithTerminatingNewline() throws Exception {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final String jsonString = StringEncoder.getString(policy.toBytes(originalEntity));

		// Assert:
		Assert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
	}

	@Test
	public void toBytesCreatesJsonStringThatCanBeRoundTripped() throws Exception {
		// Arrange:
		assertWriteCanRoundTripEntityWithString("foo");
	}

	@Test
	public void toBytesCreatesUtf8JsonStringThatCanBeRoundTripped() throws Exception {
		// Arrange:
		assertWriteCanRoundTripEntityWithString("$¢€");
	}

	private static void assertWriteCanRoundTripEntityWithString(final String str) throws Exception {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, str, 3);

		// Act:
		final String jsonString = StringEncoder.getString(policy.toBytes(originalEntity));
		final JsonDeserializer deserializer = new JsonDeserializer((JSONObject)JSONValue.parse(jsonString), null);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	//endregion

	//region fromStream

	@Test
	public void fromStreamDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);

		// Act:
		final InputStream stream = getStream(originalEntity);
		final Deserializer deserializer = policy.fromStream(stream);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void fromStreamDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(accountLookup);

		// Act:
		final InputStream stream = getStream(originalEntity);
		final Deserializer deserializer = policy.fromStream(stream);

		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromStreamFailsIfInputStringIsNotJsonObject() throws Exception {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);

		// Act:
		policy.fromStream(new ByteArrayInputStream("7".getBytes()));
	}

	//endregion

	private static InputStream getStream(final SerializableEntity entity) {
		return new ByteArrayInputStream(JsonSerializer.serializeToJson(entity).toJSONString().getBytes());
	}
}