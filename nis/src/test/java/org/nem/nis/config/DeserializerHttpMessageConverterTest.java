package org.nem.nis.config;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.test.*;
import org.springframework.http.MediaType;

import java.security.InvalidParameterException;
import java.util.List;

public class DeserializerHttpMessageConverterTest {

	// region supports / canRead / canWrite

	@Test
	public void converterSupportsApplicationJsonMediaType() {
		// Arrange:
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Act:
		final List<MediaType> mediaTypes = mc.getSupportedMediaTypes();

		// Assert:
		Assert.assertThat(mediaTypes.size(), IsEqual.equalTo(1));
		Assert.assertThat(mediaTypes.get(0).getType(), IsEqual.equalTo("application"));
		Assert.assertThat(mediaTypes.get(0).getSubtype(), IsEqual.equalTo("json"));
		Assert.assertThat(mediaTypes.get(0).getCharSet(), IsEqual.equalTo(null));
	}

	@Test
	public void cannotWriteCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Assert:
		Assert.assertThat(mc.canWrite(JsonDeserializer.class, supportedType), IsEqual.equalTo(false));
		Assert.assertThat(mc.canWrite(Deserializer.class, supportedType), IsEqual.equalTo(false));
	}

	@Test
	public void canReadCompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Assert:
		Assert.assertThat(mc.canRead(JsonDeserializer.class, supportedType), IsEqual.equalTo(true));
		Assert.assertThat(mc.canRead(Deserializer.class, supportedType), IsEqual.equalTo(true));
	}

	@Test
	public void cannotReadIncompatibleTypes() {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Assert:
		Assert.assertThat(mc.canRead(MediaType.class, supportedType), IsEqual.equalTo(false));
		Assert.assertThat(mc.canRead(Object.class, supportedType), IsEqual.equalTo(false));
	}

	//endregion

	//region read / write

	@Test(expected = UnsupportedOperationException.class)
	public void writeIsUnsupported() throws Exception {
		// Arrange:
		final MediaType supportedType = new MediaType("application", "json");
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);
		final JsonDeserializer deserializer = new JsonDeserializer(new JSONObject(), new DeserializationContext(null));

		// Act:
		mc.write(deserializer, supportedType, new MockHttpOutputMessage());
	}

	@Test
	public void readDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Act:
		final Deserializer deserializer = mc.read(
				JsonDeserializer.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		CustomAsserts.assertMockSerializableEntity(entity, 7, "foo", 3L);
	}

	@Test
	public void readDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(accountLookup);

		// Act:
		final Deserializer deserializer = mc.read(
				JsonDeserializer.class,
				new MockHttpInputMessage(JsonSerializer.serializeToJson(originalEntity)));

		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test(expected = InvalidParameterException.class)
	public void readFailsIfInputStringIsNotJsonObject() throws Exception {
		// Arrange:
		final DeserializerHttpMessageConverter mc = new DeserializerHttpMessageConverter(null);

		// Act:
		mc.read(JsonDeserializer.class, new MockHttpInputMessage("7"));
	}

	//endregion
}