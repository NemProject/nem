package org.nem.deploy;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;
import org.nem.core.utils.StringEncoder;
import org.springframework.http.MediaType;

import java.io.*;

public class JsonSerializationPolicyTest extends SerializationPolicyTest {

	//region getMediaType

	@Test
	public void policySupportsApplicationJsonMediaType() {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);

		// Act:
		final MediaType mediaType = policy.getMediaType();

		// Assert:
		MatcherAssert.assertThat(mediaType.getType(), IsEqual.equalTo("application"));
		MatcherAssert.assertThat(mediaType.getSubtype(), IsEqual.equalTo("json"));
		MatcherAssert.assertThat(mediaType.getCharset(), IsNull.nullValue());
	}

	//endregion

	//region toBytes

	@Test
	public void toBytesCreatesJsonStringWithTerminatingNewline() {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final String jsonString = StringEncoder.getString(policy.toBytes(originalEntity));

		// Assert:
		MatcherAssert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
	}

	@Test
	public void toBytesCreatesJsonStringThatCanBeRoundTripped() {
		// Arrange:
		assertWriteCanRoundTripEntityWithString("foo");
	}

	@Test
	public void toBytesCreatesUtf8JsonStringThatCanBeRoundTripped() {
		// Arrange:
		assertWriteCanRoundTripEntityWithString("$¢€");
	}

	private static void assertWriteCanRoundTripEntityWithString(final String str) {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, str, 3);

		// Act:
		final String jsonString = StringEncoder.getString(policy.toBytes(originalEntity));
		final JsonDeserializer deserializer = new JsonDeserializer((JSONObject)JSONValue.parse(jsonString), null);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		MatcherAssert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	//endregion

	//region fromStream

	@Test(expected = IllegalArgumentException.class)
	public void fromStreamFailsIfInputStringIsNotJsonObject() {
		// Arrange:
		final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);

		// Act:
		policy.fromStream(new ByteArrayInputStream("7".getBytes()));
	}

	//endregion

	@Override
	protected SerializationPolicy createPolicy(final AccountLookup accountLookup) {
		return new JsonSerializationPolicy(accountLookup);
	}

	@Override
	protected InputStream createStream(final SerializableEntity entity) {
		return new ByteArrayInputStream(JsonSerializer.serializeToBytes(entity));
	}
}