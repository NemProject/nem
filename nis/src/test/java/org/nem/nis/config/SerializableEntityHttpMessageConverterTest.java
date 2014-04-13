package org.nem.nis.config;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.test.*;
import org.springframework.http.MediaType;

import java.util.List;

public class SerializableEntityHttpMessageConverterTest {

	// region supports / canRead / canWrite

	@Test
	public void converterSupportsApplicationJsonMediaType() {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();

		// Act:
		final List<MediaType> mediaTypes = mc.getSupportedMediaTypes();

		// Assert:
		Assert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		Assert.assertThat(mediaTypes.get(0).getType(), IsEqual.equalTo("application"));
		Assert.assertThat(mediaTypes.get(0).getSubtype(), IsEqual.equalTo("json"));
		Assert.assertThat(mediaTypes.get(0).getCharSet(), IsEqual.equalTo(null));
	}
	@Test
	public void canWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();

		// Assert:
		Assert.assertThat(mc.canWrite(MockSerializableEntity.class, supportedType), IsEqual.equalTo(true));
		Assert.assertThat(mc.canWrite(SerializableEntity.class, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotWriteIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();

		// Assert:
		Assert.assertThat(mc.canWrite(MediaType.class, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void cannotReadCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();

		// Assert:
		Assert.assertThat(mc.canRead(MockSerializableEntity.class, supportedType), IsEqual.equalTo(false));
		Assert.assertThat(mc.canRead(SerializableEntity.class, supportedType), IsEqual.equalTo(false));
	}


	//endregion

	//region read / write

	@Test(expected = UnsupportedOperationException.class)
	public void readIsUnsupported() throws Exception {
		// Arrange:
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		mc.read(
				MockSerializableEntity.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));
	}

	@Test
	public void writeCreatesJsonStringWithTerminatingNewline() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();
		final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		// Act:
		mc.write(originalEntity, supportedType, outputMessage);
		final String jsonString = outputMessage.getBodyAsString();

		// Assert:
		Assert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
	}

	@Test
	public void writeCreatesJsonStringThatCanBeRoundTripped() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializableEntityHttpMessageConverter mc = new SerializableEntityHttpMessageConverter();
		final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		// Act:
		mc.write(originalEntity, supportedType, outputMessage);
		final String jsonString = outputMessage.getBodyAsString();
		final JsonDeserializer deserializer = new JsonDeserializer((JSONObject)JSONValue.parse(jsonString), null);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		CustomAsserts.assertMockSerializableEntity(entity, 7, "foo", 3L);
	}

	//endregion
}
