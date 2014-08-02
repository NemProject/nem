package org.nem.core.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.springframework.http.MediaType;

import java.io.*;

public class BinarySerializationPolicyTest {

	//region getMediaType

	@Test
	public void policySupportsApplicationBinaryMediaType() {
		// Arrange:
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(null);

		// Act:
		final MediaType mediaType = policy.getMediaType();

		// Assert:
		Assert.assertThat(mediaType.getType(), IsEqual.equalTo("application"));
		Assert.assertThat(mediaType.getSubtype(), IsEqual.equalTo("binary"));
		Assert.assertThat(mediaType.getCharSet(), IsNull.nullValue());
	}

	//endregion

	//region toBytes

	@Test
	public void toBytesDelegatesToBinarySerializer() throws Exception {
		// Arrange:
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final byte[] bytes = policy.toBytes(originalEntity);

		// Assert:
		Assert.assertThat(
				bytes,
				IsEqual.equalTo(BinarySerializer.serializeToBytes(originalEntity)));
	}

	//endregion

	//region fromStream

	@Test
	public void fromStreamDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(null);

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
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(accountLookup);

		// Act:
		final InputStream stream = getStream(originalEntity);
		final Deserializer deserializer = policy.fromStream(stream);

		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	//endregion

	private static InputStream getStream(final SerializableEntity entity) {
		return new ByteArrayInputStream(BinarySerializer.serializeToBytes(entity));
	}
}