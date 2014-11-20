package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class RequestPrepareAnnounceTest {

	@Test
	public void canCreateRequest() {
		// Arrange:
		final Transaction transaction = createTransfer();
		final PrivateKey privateKey = new KeyPair().getPrivateKey();
		final RequestPrepareAnnounce request = new RequestPrepareAnnounce(transaction, privateKey);

		// Assert:
		Assert.assertThat(request.getTransaction(), IsEqual.equalTo(transaction));
		Assert.assertThat(request.getPrivateKey(), IsEqual.equalTo(privateKey));
	}

	@Test
	public void canRoundTripRequest() {
		// Arrange:
		final Transaction transaction = createTransfer();
		final Hash transactionHash = HashUtils.calculateHash(transaction.asNonVerifiable());
		final PrivateKey privateKey = new KeyPair().getPrivateKey();
		final RequestPrepareAnnounce originalRequest = new RequestPrepareAnnounce(transaction, privateKey);

		// Act:
		final RequestPrepareAnnounce request = createRoundTrippedRequest(originalRequest);

		// Assert:
		Assert.assertThat(HashUtils.calculateHash(request.getTransaction().asNonVerifiable()), IsEqual.equalTo(transactionHash));
		Assert.assertThat(request.getPrivateKey(), IsEqual.equalTo(privateKey));
	}

	private static RequestPrepareAnnounce createRoundTrippedRequest(final RequestPrepareAnnounce originalRequest) {
		// Act:
		return new RequestPrepareAnnounce(Utils.roundtripSerializableEntity(originalRequest, new MockAccountLookup()));
	}

	private static TransferTransaction createTransfer() {
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				new Amount(456),
				null);
	}
}