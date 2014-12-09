package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.nis.AccountCache;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.Account;

import java.util.function.Consumer;

public class AutoMapperTest {

	@Test
	public void canMapTransaction() {
		// Arrange:
		final AutoMapper mapper = new AutoMapper();
		mapper.addMapping(
				Transfer.class,
				org.nem.core.model.TransferTransaction.class,
				new TransferToTransactionMapping(mapper));
		mapper.addMapping(
				org.nem.nis.dbmodel.Account.class,
				org.nem.core.model.Account.class,
				new DbAccountToAccount(new MockAccountLookup()));

		final Address senderAddress = Utils.generateRandomAddressWithPublicKey();
		final Signature signature = Utils.generateRandomSignature();
		final Hash hash = Utils.generateRandomHash();
		final Address recipientAddress = Utils.generateRandomAddress();
		final Transfer transfer = new Transfer();
		transfer.setFee(123000000L);
		transfer.setTimeStamp(1856002);
		transfer.setSender(new Account(senderAddress.getEncoded(), senderAddress.getPublicKey()));
		transfer.setSenderProof(signature.getBytes());
		transfer.setTransferHash(hash);
		transfer.setDeadline(1856002 + 1000);

		transfer.setRecipient(new Account(recipientAddress.getEncoded(), null));
		transfer.setAmount(888888000000L);
		//transfer.setMessageType(2);
		//transfer.setMessagePayload(messagePayload);

		// Act:
		final TransferTransaction transaction = mapper.map(transfer, TransferTransaction.class);


	}


	@Test
	public void mapperSpeedTest() {final Address senderAddress = Utils.generateRandomAddressWithPublicKey();
		final Signature signature = Utils.generateRandomSignature();
		final Hash hash = Utils.generateRandomHash();
		final Address recipientAddress = Utils.generateRandomAddress();
		final Transfer transfer = new Transfer();
		transfer.setFee(123000000L);
		transfer.setTimeStamp(1856002);
		transfer.setSender(new Account(senderAddress.getEncoded(), senderAddress.getPublicKey()));
		transfer.setSenderProof(signature.getBytes());
		transfer.setTransferHash(hash);
		transfer.setDeadline(1856002 + 1000);

		transfer.setRecipient(new Account(recipientAddress.getEncoded(), null));
		transfer.setAmount(888888000000L);
		//transfer.setMessageType(2);
		//transfer.setMessagePayload(messagePayload);

		System.out.println("testing TransferMapper.toModel");
		speedTest(v -> {
			final TransferTransaction model = TransferMapper.toModel(transfer, new MockAccountLookup());
			Assert.assertThat(model.getAmount(), IsEqual.equalTo(new Amount(888888000000L)));
		});

		System.out.println("testing AutoMapper");
		speedTest(v -> {
			final AutoMapper mapper = new AutoMapper();
			mapper.addMapping(
					Transfer.class,
					org.nem.core.model.TransferTransaction.class,
					new TransferToTransactionMapping(mapper));
			mapper.addMapping(
					org.nem.nis.dbmodel.Account.class,
					org.nem.core.model.Account.class,
					new DbAccountToAccount(new MockAccountLookup()));

			final TransferTransaction model = mapper.map(transfer, TransferTransaction.class);
			Assert.assertThat(model.getAmount(), IsEqual.equalTo(new Amount(888888000000L)));
		});
	}

	private static void speedTest(final Consumer<Void> action) {

		for (int i = 0; i < 5000; ++i) {
			action.accept(null);
		}

		final long start = System.currentTimeMillis();

		for (int i = 0; i < 1000000; ++i) {
			action.accept(null);
		}

		final long stop = System.currentTimeMillis();

		System.out.println(String.format("elapsed: %d", stop - start));
	}

}