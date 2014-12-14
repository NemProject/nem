package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.HashShortId;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsRequestTest {

	// region construction

	@Test
	public void canCreateUnconfirmedTransactionsRequestWithoutParameters() {
		// Act:
		final UnconfirmedTransactionsRequest request = new UnconfirmedTransactionsRequest();

		// Assert:
		Assert.assertThat(request.getHashShortIds().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateUnconfirmedTransactionsRequestAroundEmptyList() {
		// Act:
		final UnconfirmedTransactionsRequest request = new UnconfirmedTransactionsRequest(new ArrayList<>());

		// Assert:
		Assert.assertThat(request.getHashShortIds().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateUnconfirmedTransactionsRequestAroundNonEmptyList() {
		// Act:
		final TestContext context = new TestContext(10);

		// Assert:
		Assert.assertThat(context.request.getHashShortIds(), IsEquivalent.equivalentTo(context.hashShortIds));
	}

	// endregion

	//region serialization

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(10);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(context.request, null);
		final UnconfirmedTransactionsRequest request = new UnconfirmedTransactionsRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getHashShortIds(), IsEquivalent.equivalentTo(context.hashShortIds));
	}

	// endregion

	private class TestContext {
		private final List<Transaction> transactions;
		private final List<HashShortId> hashShortIds;
		private final UnconfirmedTransactionsRequest request;

		private TestContext(final int count) {
			this.transactions = this.createTransactions(count);
			this.request = new UnconfirmedTransactionsRequest(this.transactions);
			this.hashShortIds = transactions.stream()
					.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
					.collect(Collectors.toList());
		}

		private List<Transaction> createTransactions(final int count) {
			final List<Transaction> list = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				list.add(new MockTransaction(new TimeInstant(i)));
			}

			return list;
		}
	}
}
