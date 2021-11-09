package org.nem.deploy;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;
import org.nem.deploy.test.*;
import org.springframework.http.MediaType;

import java.util.List;

public class DeserializerHttpMessageConverterTest {

	// region supports / canRead / canWrite

	@Test
	public void converterSupportsPolicyMediaType() {
		// Arrange:
		final MediaType mediaType = new MediaType("application");
		final SerializationPolicy policy = Mockito.mock(SerializationPolicy.class);
		Mockito.when(policy.getMediaType()).thenReturn(mediaType);
		final DeserializerHttpMessageConverter mc = createMessageConverter(policy);

		// Act:
		final List<MediaType> mediaTypes = mc.getSupportedMediaTypes();

		// Assert:
		MatcherAssert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mediaTypes.get(0), IsEqual.equalTo(mediaType));
		Mockito.verify(policy, Mockito.times(1)).getMediaType();
	}

	@Test
	public void canReadCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canRead(JsonDeserializer.class, supportedType), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mc.canRead(Deserializer.class, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotReadIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canRead(MediaType.class, supportedType), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mc.canRead(Object.class, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void cannotWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = createMessageConverter();

		// Assert:
		MatcherAssert.assertThat(mc.canWrite(JsonDeserializer.class, supportedType), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mc.canWrite(Deserializer.class, supportedType), IsEqual.equalTo(false));
	}

	// endregion

	// region read

	@Test
	public void readDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final DeserializerHttpMessageConverter mc = createMessageConverter();

		// Act:
		final Deserializer deserializer = mc.read(JsonDeserializer.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

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
		final DeserializerHttpMessageConverter mc = createMessageConverter(policy);

		// Act:
		final MockHttpInputMessage message = new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity));
		mc.read(JsonDeserializer.class, message);

		// Assert:
		Mockito.verify(policy, Mockito.times(1)).fromStream(message.getBody());
	}

	// endregion

	// region write

	@Test(expected = UnsupportedOperationException.class)
	public void writeIsUnsupported() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = createMessageConverter();
		final JsonDeserializer deserializer = new JsonDeserializer(new JSONObject(), new DeserializationContext(null));

		// Act:
		mc.write(deserializer, supportedType, new MockHttpOutputMessage());
	}

	// endregion

	private static DeserializerHttpMessageConverter createMessageConverter() {
		return new DeserializerHttpMessageConverter(new JsonSerializationPolicy(null));
	}

	private static DeserializerHttpMessageConverter createMessageConverter(final SerializationPolicy policy) {
		return new DeserializerHttpMessageConverter(policy);
	}
}
