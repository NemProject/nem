package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.*;

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

	// region signatures of multisig transactions

	// TODO 20150327 J-B: this is the same test as the previous
	// TODO 20150327 BR _> J: right, i used the wrong constructor.
	@Test
	public void signatureTransactionsAreHandlesAsOwnEntities() {
		// Act:
		final TestContext context = new TestContext();

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
			this.hashShortIds = this.transactions.stream()
					.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
					.collect(Collectors.toList());
		}

		private TestContext() {
			this.hashShortIds = new ArrayList<>();
			this.transactions = Arrays.asList(this.createMultiSigTransactionWithSignatures());
			this.request = new UnconfirmedTransactionsRequest(this.transactions);
		}

		private List<Transaction> createTransactions(final int count) {
			final List<Transaction> list = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				list.add(new MockTransaction(new TimeInstant(i)));
			}

			return list;
		}

		private MultisigTransaction createMultiSigTransactionWithSignatures() {
			final Account multisig = Utils.generateRandomAccount();
			final TransferTransaction otherTransaction = new TransferTransaction(
					TimeInstant.ZERO,
					multisig,
					Utils.generateRandomAccount(),
					Amount.fromNem(123),
					null);
			final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), otherTransaction);
			transaction.sign();
			this.hashShortIds.add(new HashShortId(HashUtils.calculateHash(transaction).getShortId()));
			IntStream.range(0, 3).forEach(i -> {
				final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
						TimeInstant.ZERO,
						Utils.generateRandomAccount(),
						multisig,
						transaction.getOtherTransaction());
				transaction.addSignature(signature);
				this.hashShortIds.add(new HashShortId(HashUtils.calculateHash(signature).getShortId()));
			});

			return transaction;
		}
	}
}
