package org.nem.core.model.ncc;

import org.hamcrest.MatcherAssert;
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
		MatcherAssert.assertThat(request.getTransaction(), IsEqual.equalTo(transaction));
		MatcherAssert.assertThat(request.getPrivateKey(), IsEqual.equalTo(privateKey));
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
		MatcherAssert.assertThat(HashUtils.calculateHash(request.getTransaction().asNonVerifiable()), IsEqual.equalTo(transactionHash));
		MatcherAssert.assertThat(request.getPrivateKey(), IsEqual.equalTo(privateKey));
	}

	private static RequestPrepareAnnounce createRoundTrippedRequest(final RequestPrepareAnnounce originalRequest) {
		// Act:
		return new RequestPrepareAnnounce(Utils.roundtripSerializableEntity(originalRequest, new MockAccountLookup()));
	}

	private static TransferTransaction createTransfer() {
		return new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), Utils.generateRandomAccount(), new Amount(456),
				null);
	}
}
