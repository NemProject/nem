package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.*;

public class TransferTransactionAttachmentTest {

	//region constructor

	@Test
	public void canCreateEmptyAttachment() {
		// Act:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsNull.nullValue());
		Assert.assertThat(attachment.getMosaicTransfers().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateEmptyAttachmentWithMessage() {
		// Act:
		final Message message = new PlainMessage(Utils.generateRandomBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsEqual.equalTo(message));
		Assert.assertThat(attachment.getMosaicTransfers().isEmpty(), IsEqual.equalTo(true));
	}

	//endregion

	//region message

	@Test
	public void canSetMessage() {
		// Arrange:
		final Message message = new PlainMessage(Utils.generateRandomBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.setMessage(message);

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsEqual.equalTo(message));
	}

	@Test
	public void cannotResetMessage() {
		// Arrange:
		final Message message1 = new PlainMessage(Utils.generateRandomBytes());
		final Message message2 = new PlainMessage(Utils.generateRandomBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.setMessage(message1);

		// Act:
		ExceptionAssert.assertThrows(
				v -> attachment.setMessage(message2),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsEqual.equalTo(message1));
	}

	//endregion

	//region asset transfers

	@Test
	public void canAddMosaicTransfers() {
		// Arrange:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.addMosaicTransfer(Utils.createMosaicId(1), new Quantity(12));
		attachment.addMosaicTransfer(Utils.createMosaicId(2), new Quantity(77));
		attachment.addMosaicTransfer(Utils.createMosaicId(3), new Quantity(41));

		// Assert:
		final Collection<MosaicTransferPair> expectedPairs = Arrays.asList(
				new MosaicTransferPair(Utils.createMosaicId(1), new Quantity(12)),
				new MosaicTransferPair(Utils.createMosaicId(2), new Quantity(77)),
				new MosaicTransferPair(Utils.createMosaicId(3), new Quantity(41)));
		Assert.assertThat(attachment.getMosaicTransfers(), IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void addMosaicTransfersAreCumulative() {
		// Arrange:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.addMosaicTransfer(Utils.createMosaicId(1), new Quantity(12));
		attachment.addMosaicTransfer(Utils.createMosaicId(1), new Quantity(77));
		attachment.addMosaicTransfer(Utils.createMosaicId(1), new Quantity(41));

		// Assert:
		final Collection<MosaicTransferPair> expectedPairs = Collections.singletonList(
				new MosaicTransferPair(Utils.createMosaicId(1), new Quantity(130)));
		Assert.assertThat(attachment.getMosaicTransfers(), IsEquivalent.equivalentTo(expectedPairs));
	}

	//endregion
}