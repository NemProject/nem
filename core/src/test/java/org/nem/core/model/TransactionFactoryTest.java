package org.nem.core.model;

import java.util.Collection;
import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

@RunWith(Enclosed.class)
public class TransactionFactoryTest {

	public static class General {

		// region size / isSupported

		@Test
		public void allExpectedTransactionTypesAreSupported() {
			// Assert:
			MatcherAssert.assertThat(TransactionFactory.size(), IsEqual.equalTo(TransactionTypes.getActiveTypes().size()));
		}

		@Test
		public void isSupportedReturnsTrueForSupportedTypes() {
			// Arrange:
			final Collection<Integer> expectedRegisteredTypes = TransactionTypes.getActiveTypes();

			// Act:
			for (final Integer type : expectedRegisteredTypes) {
				// Act:
				final boolean isSupported = TransactionFactory.isSupported(type);

				// Assert:
				MatcherAssert.assertThat(isSupported, IsEqual.equalTo(true));
			}

			MatcherAssert.assertThat(expectedRegisteredTypes.size(), IsEqual.equalTo(TransactionFactory.size()));
		}

		@Test
		public void isSupportedReturnsFalseForUnsupportedTypes() {
			// Assert:
			MatcherAssert.assertThat(TransactionFactory.isSupported(9999), IsEqual.equalTo(false));
			MatcherAssert.assertThat(TransactionFactory.isSupported(TransactionTypes.TRANSFER | 0x1000), IsEqual.equalTo(false));
		}

		// endregion

		// region Unknown Transaction Type

		@Test(expected = IllegalArgumentException.class)
		public void cannotDeserializeUnknownTransaction() {
			// Arrange:
			final JSONObject object = new JSONObject();
			object.put("type", 7);
			final JsonDeserializer deserializer = new JsonDeserializer(object, null);

			// Act:
			TransactionFactory.VERIFIABLE.deserialize(deserializer);
		}

		// endregion
	}

	// region PerTransaction

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TestTransactionRegistry.Entry<?> entry;

		public PerTransaction(final int type) {
			this.entry = TestTransactionRegistry.findByType(type);
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getActiveTypes());
		}

		@Test
		public void canDeserializeVerifiableTransaction() {
			// Arrange:
			final Transaction originalTransaction = this.entry.createModel.get();

			// Assert:
			assertCanDeserializeVerifiable(originalTransaction, this.entry.modelClass, this.entry.type);
		}

		@Test
		public void canDeserializeNonVerifiableTransaction() {
			// Arrange:
			final Transaction originalTransaction = this.entry.createModel.get();

			// Assert:
			assertCanDeserializeNonVerifiable(originalTransaction, this.entry.modelClass, this.entry.type);
		}

		private static void assertCanDeserializeVerifiable(final Transaction originalTransaction, final Class<?> expectedClass,
				final int expectedType) {
			// Act:
			final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());
			final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

			// Assert:
			MatcherAssert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
			MatcherAssert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
			MatcherAssert.assertThat(transaction.getSignature(), IsNull.notNullValue());
		}

		private static void assertCanDeserializeNonVerifiable(final Transaction originalTransaction, final Class<?> expectedClass,
				final int expectedType) {
			// Act:
			final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(),
					new MockAccountLookup());
			final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

			// Assert:
			MatcherAssert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
			MatcherAssert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
			MatcherAssert.assertThat(transaction.getSignature(), IsNull.nullValue());
		}
	}

	// endregion
}
