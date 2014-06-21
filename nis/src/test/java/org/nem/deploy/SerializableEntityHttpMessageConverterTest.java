package org.nem.deploy;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.test.*;
import org.springframework.http.MediaType;

import java.io.IOException;
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
		Assert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		Assert.assertThat(mediaTypes.get(0), IsEqual.equalTo(mediaType));
		Mockito.verify(policy, Mockito.times(2)).getMediaType();
	}

	@Test
	public void canWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		Assert.assertThat(mc.canWrite(MockSerializableEntity.class, supportedType), IsEqual.equalTo(true));
		Assert.assertThat(mc.canWrite(SerializableEntity.class, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotWriteIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		Assert.assertThat(mc.canWrite(MediaType.class, supportedType), IsEqual.equalTo(false));
		Assert.assertThat(mc.canWrite(Object.class, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void canReadCompatibleTypesWithDeserializerConstructor() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		final Class[] types = new Class[] {
				MockSerializableEntity.class,
				SerializableEntityWithConstructorThatThrowsCheckedException.class,
				SerializableEntityWithConstructorThatThrowsUncheckedException.class
		};

		// Assert:
		for (final Class type : types)
			Assert.assertThat(mc.canRead(type, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotReadCompatibleTypesWithoutDeserializerConstructor() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		final Class[] types = new Class[] {
				SerializableEntity.class,
				SerializableEntityWithoutDeserializerConstructor.class
		};

		// Assert:
		for (final Class type : types)
			Assert.assertThat(mc.canRead(type, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void cannotReadIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Assert:
		Assert.assertThat(mc.canRead(MediaType.class, supportedType), IsEqual.equalTo(false));
		Assert.assertThat(mc.canRead(Object.class, supportedType), IsEqual.equalTo(false));
	}

	//endregion

	//region read

	@Test
	public void readIsSupportedForCompatibleTypeWithDeserializerConstructor() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Act:
		final MockSerializableEntity entity = (MockSerializableEntity)mc.read(
				MockSerializableEntity.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void readDelegatesToPolicy() throws Exception {
		// Arrange:
		final MediaType mediaType = new MediaType("application", "json");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);
		Mockito.when(policy.fromStream(Mockito.any())).thenReturn(Mockito.mock(Deserializer.class));

		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = createMessageConverter(policy);

		// Act:
		final MockHttpInputMessage message = new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity));
		mc.read(MockSerializableEntity.class, message);

		// Assert:
		Mockito.verify(policy, Mockito.times(1)).fromStream(message.getBody());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupportedForCompatibleTypeWithConstructorThatThrowsCheckedException() throws Exception {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				SerializableEntityWithConstructorThatThrowsCheckedException.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void readIsUnsupportedForCompatibleTypeWithConstructorThatThrowsUncheckedException() throws Exception {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				SerializableEntityWithConstructorThatThrowsUncheckedException.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupportedForCompatibleTypeWithoutDeserializerConstructor() throws Exception {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				SerializableEntityWithoutDeserializerConstructor.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	//endregion

	//region write


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
		Assert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
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
		final JsonDeserializer deserializer = new JsonDeserializer((JSONObject)JSONValue.parse(jsonString), null);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
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

	//endregion

	private static SerializableEntityHttpMessageConverter createMessageConverter() {
		return createMessageConverter(new JsonSerializationPolicy(null));
	}

	private static SerializableEntityHttpMessageConverter createMessageConverter(final SerializationPolicy policy) {
		return new SerializableEntityHttpMessageConverter(
				new DeserializerHttpMessageConverter(policy),
				policy);
	}

	private static class SerializableEntityWithoutDeserializerConstructor extends MockSerializableEntity {

		public SerializableEntityWithoutDeserializerConstructor() {
		}
	}

	private static class SerializableEntityWithConstructorThatThrowsUncheckedException extends MockSerializableEntity{

		public SerializableEntityWithConstructorThatThrowsUncheckedException(final Deserializer deserializer) {
			throw new IllegalArgumentException("constructor failed: " + deserializer.toString());
		}
	}

	private static class SerializableEntityWithConstructorThatThrowsCheckedException extends MockSerializableEntity{

		public SerializableEntityWithConstructorThatThrowsCheckedException(final Deserializer deserializer) throws IOException {
			throw new IOException("constructor failed: " + deserializer.toString());
		}
	}
}