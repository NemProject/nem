package org.nem.deploy;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;
import org.nem.deploy.test.*;
import org.springframework.http.MediaType;

import java.util.List;

public class SerializableEntityHttpMessageConverterTest {

	// region supports / canRead / canWrite

	@Test
	public void converterSupportsPolicyMediaType() {
		// Arrange:
		final MediaType mediaType = new MediaType("application");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter(policy);

		// Act:
		final List<MediaType> mediaTypes = mc.getSupportedMediaTypes();

		// Assert:
		MatcherAssert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mediaTypes.get(0), IsEqual.equalTo(mediaType));
		Mockito.verify(policy, Mockito.times(1)).getMediaType();
	}

	@Test
	public void cannotReadCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canRead(MockSerializableEntity.class, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void canWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canWrite(MockSerializableEntity.class, supportedType), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mc.canWrite(SerializableEntity.class, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotWriteIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canWrite(MediaType.class, supportedType), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mc.canWrite(Object.class, supportedType), IsEqual.equalTo(false));
	}

	// endregion

	// region read

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupported() throws Exception {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(MockSerializableEntity.class, new MockHttpInputMessage(new JSONObject()));
	}

	// endregion

	// region write

	@Test
	public void writeCreatesJsonStringWithTerminatingNewline() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();
		final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		// Act:
		mc.write(originalEntity, supportedType, outputMessage);
		final String jsonString = outputMessage.getBodyAsString();

		// Assert:
		MatcherAssert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
	}

	@Test
	public void writeCreatesUtf8JsonStringThatCanBeRoundTripped() throws Exception {
		// Arrange:
		assertWriteCanRoundTripEntityWithString("$¢€");
	}

	private static void assertWriteCanRoundTripEntityWithString(final String str) throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, str, 3);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();
		final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		// Act:
		mc.write(originalEntity, supportedType, outputMessage);
		final String jsonString = outputMessage.getBodyAsString();
		final JsonDeserializer deserializer = new JsonDeserializer((JSONObject) JSONValue.parse(jsonString), null);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		MatcherAssert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void writeDelegatesToPolicy() throws Exception {
		// Arrange:
		final MediaType mediaType = new MediaType("application", "json");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);
		Mockito.when(policy.toBytes(Mockito.any())).thenReturn(new byte[0]);

		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter(policy);
		final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		// Act:
		mc.write(originalEntity, mediaType, outputMessage);

		// Assert:
		Mockito.verify(policy, Mockito.times(1)).toBytes(originalEntity);
	}

	// endregion

	private static SerializableEntityHttpMessageConverter createMessageConverter() {
		return createMessageConverter(new JsonSerializationPolicy(null));
	}

	private static SerializableEntityHttpMessageConverter createMessageConverter(final SerializationPolicy policy) {
		return new SerializableEntityHttpMessageConverter(policy);
	}
}
