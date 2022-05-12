package org.nem.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;

/**
 * Base class for SerializationPolicy tests.
 */
public abstract class SerializationPolicyTest {

	/**
	 * Creates a policy to test.
	 *
	 * @param accountLookup The account lookup.
	 * @return The policy.
	 */
	protected abstract SerializationPolicy createPolicy(final AccountLookup accountLookup);

	/**
	 * Creates a compatible entity stream.
	 *
	 * @param entity The entity.
	 * @return The stream.
	 */
	protected abstract InputStream createStream(final SerializableEntity entity);

	@Test
	public void fromStreamDeserializerIsCorrectlyCreatedAroundInput() {
		// Assert:
		this.assertCorrectRoundtripWithString("foo");
	}

	@Test
	public void fromStreamDeserializerIsCorrectlyCreatedAroundInputWithUnicodeStrings() {
		// Assert:
		this.assertCorrectRoundtripWithString("zuação danada");
	}

	private void assertCorrectRoundtripWithString(final String s) {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, s, 3);
		final SerializationPolicy policy = this.createPolicy(null);

		// Act:
		final InputStream stream = this.createStream(originalEntity);
		final Deserializer deserializer = policy.fromStream(stream);
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		MatcherAssert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void fromStreamDeserializerIsAssociatedWithAccountLookup() {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		final SerializationPolicy policy = this.createPolicy(accountLookup);

		// Act:
		final InputStream stream = this.createStream(originalEntity);
		final Deserializer deserializer = policy.fromStream(stream);

		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		MatcherAssert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}
}
