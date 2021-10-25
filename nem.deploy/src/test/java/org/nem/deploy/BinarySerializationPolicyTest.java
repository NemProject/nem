package org.nem.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;
import org.springframework.http.MediaType;

import java.io.*;

public class BinarySerializationPolicyTest extends SerializationPolicyTest {

	//region getMediaType

	@Test
	public void policySupportsApplicationBinaryMediaType() {
		// Arrange:
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(null);

		// Act:
		final MediaType mediaType = policy.getMediaType();

		// Assert:
		MatcherAssert.assertThat(mediaType.getType(), IsEqual.equalTo("application"));
		MatcherAssert.assertThat(mediaType.getSubtype(), IsEqual.equalTo("binary"));
		MatcherAssert.assertThat(mediaType.getCharset(), IsNull.nullValue());
	}

	//endregion

	//region toBytes

	@Test
	public void toBytesDelegatesToBinarySerializer() {
		// Arrange:
		final BinarySerializationPolicy policy = new BinarySerializationPolicy(null);
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final byte[] bytes = policy.toBytes(originalEntity);

		// Assert:
		MatcherAssert.assertThat(
				bytes,
				IsEqual.equalTo(BinarySerializer.serializeToBytes(originalEntity)));
	}

	//endregion

	@Override
	protected SerializationPolicy createPolicy(final AccountLookup accountLookup) {
		return new BinarySerializationPolicy(accountLookup);
	}

	@Override
	protected InputStream createStream(final SerializableEntity entity) {
		return new ByteArrayInputStream(BinarySerializer.serializeToBytes(entity));
	}
}