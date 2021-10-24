package org.nem.deploy;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.deploy.test.*;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

public class DeserializableEntityMessageConverterTest {

	// region supports / canRead / canWrite

	@Test
	public void converterSupportsPolicyMediaType() {
		// Arrange:
		final MediaType mediaType = new MediaType("application");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);
		final DeserializableEntityMessageConverter mc = createMessageConverter(policy);

		// Act:
		final List<MediaType> mediaTypes = mc.getSupportedMediaTypes();

		// Assert:
		MatcherAssert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mediaTypes.get(0), IsEqual.equalTo(mediaType));
		Mockito.verify(policy, Mockito.times(2)).getMediaType();
	}

	@Test
	public void canReadCompatibleTypesWithDeserializerConstructor() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		final Class[] types = new Class[] {
				MockSerializableEntity.class,
				ObjectWithConstructorThatThrowsCheckedException.class,
				ObjectWithConstructorThatThrowsUncheckedException.class
		};

		// Assert:
		for (final Class type : types) {
			MatcherAssert.assertThat(mc.canRead(type, supportedType), IsEqual.equalTo(true));
		}
	}

	@Test
	public void cannotReadTypesWithoutDeserializerConstructor() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		final Class[] types = new Class[] {
				SerializableEntity.class,
				ObjectWithoutDeserializerConstructor.class,
				MediaType.class,
				Object.class
		};

		// Assert:
		for (final Class type : types) {
			MatcherAssert.assertThat(mc.canRead(type, supportedType), IsEqual.equalTo(false));
		}
	}

	@Test
	public void cannotReadIncompatibleMediaTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "binary");
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		final Class[] types = new Class[] {
				MockSerializableEntity.class,
				ObjectWithConstructorThatThrowsCheckedException.class,
				ObjectWithConstructorThatThrowsUncheckedException.class
		};

		// Assert:
		for (final Class type : types) {
			MatcherAssert.assertThat(mc.canRead(type, supportedType), IsEqual.equalTo(false));
		}
	}

	@Test
	public void cannotWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canWrite(MockSerializableEntity.class, supportedType), IsEqual.equalTo(false));
	}

	//endregion

	//region read

	@Test
	public void readIsSupportedForCompatibleTypeWithDeserializerConstructor() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		// Act:
		final MockSerializableEntity entity = (MockSerializableEntity)mc.read(
				MockSerializableEntity.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));

		// Assert:
		MatcherAssert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void readDelegatesToPolicy() throws Exception {
		// Arrange:
		final MediaType mediaType = new MediaType("application", "json");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);

		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, null);
		Mockito.when(policy.fromStream(Mockito.any())).thenReturn(deserializer);

		final DeserializableEntityMessageConverter mc = createMessageConverter(policy);

		// Act:
		final MockHttpInputMessage message = new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity));
		mc.read(MockSerializableEntity.class, message);

		// Assert:
		Mockito.verify(policy, Mockito.times(1)).fromStream(message.getBody());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupportedForCompatibleTypeWithConstructorThatThrowsCheckedException() throws Exception {
		// Arrange:
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				ObjectWithConstructorThatThrowsCheckedException.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void readIsUnsupportedForCompatibleTypeWithConstructorThatThrowsUncheckedException() throws Exception {
		// Arrange:
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				ObjectWithConstructorThatThrowsUncheckedException.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupportedForTypeWithoutDeserializerConstructor() throws Exception {
		// Arrange:
		final DeserializableEntityMessageConverter mc = createMessageConverter();

		// Act:
		mc.read(
				ObjectWithoutDeserializerConstructor.class,
				new MockHttpInputMessage(new JSONObject()));
	}

	//endregion

	//region write

	@Test(expected = UnsupportedOperationException.class)
	public void writeIsUnsupported() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializableEntityMessageConverter mc = createMessageConverter();
		final JsonDeserializer deserializer = new JsonDeserializer(new JSONObject(), new DeserializationContext(null));

		// Act:
		mc.write(deserializer, supportedType, new MockHttpOutputMessage());
	}

	//endregion

	//region test classes

	private static class ObjectWithoutDeserializerConstructor {

		public ObjectWithoutDeserializerConstructor() {
		}
	}

	private static class ObjectWithConstructorThatThrowsUncheckedException {

		public ObjectWithConstructorThatThrowsUncheckedException(final Deserializer deserializer) {
			throw new IllegalArgumentException("constructor failed: " + deserializer.toString());
		}
	}

	private static class ObjectWithConstructorThatThrowsCheckedException {

		public ObjectWithConstructorThatThrowsCheckedException(final Deserializer deserializer) throws IOException {
			throw new IOException("constructor failed: " + deserializer.toString());
		}
	}

	//endregion

	private static DeserializableEntityMessageConverter createMessageConverter() {
		return new DeserializableEntityMessageConverter(new JsonSerializationPolicy(null));
	}

	private static DeserializableEntityMessageConverter createMessageConverter(final SerializationPolicy policy) {
		return new DeserializableEntityMessageConverter(policy);
	}
}