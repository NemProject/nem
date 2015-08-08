package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class TransferTransactionAttachmentTest {

	//region constructor

	@Test
	public void canCreateEmptyAttachment() {
		// Act:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsNull.nullValue());
		Assert.assertThat(attachment.getMosaics().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateEmptyAttachmentWithMessage() {
		// Act:
		final Message message = new PlainMessage(Utils.generateRandomBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);

		// Assert:
		Assert.assertThat(attachment.getMessage(), IsEqual.equalTo(message));
		Assert.assertThat(attachment.getMosaics().isEmpty(), IsEqual.equalTo(true));
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

	//region mosaics

	@Test
	public void canAddMosaics() {
		// Arrange:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.addMosaic(Utils.createMosaicId(1), new Quantity(12));
		attachment.addMosaic(new Mosaic(Utils.createMosaicId(2), new Quantity(77)));
		attachment.addMosaic(Utils.createMosaicId(3), new Quantity(41));

		// Assert:
		final Collection<Mosaic> expectedPairs = Arrays.asList(
				new Mosaic(Utils.createMosaicId(1), new Quantity(12)),
				new Mosaic(Utils.createMosaicId(2), new Quantity(77)),
				new Mosaic(Utils.createMosaicId(3), new Quantity(41)));
		Assert.assertThat(attachment.getMosaics(), IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void mosaicAdditionsAreCumulative() {
		// Arrange:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.addMosaic(Utils.createMosaicId(1), new Quantity(12));
		attachment.addMosaic(new Mosaic(Utils.createMosaicId(1), new Quantity(77)));
		attachment.addMosaic(Utils.createMosaicId(1), new Quantity(41));

		// Assert:
		final Collection<Mosaic> expectedPairs = Collections.singletonList(
				new Mosaic(Utils.createMosaicId(1), new Quantity(130)));
		Assert.assertThat(attachment.getMosaics(), IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void mosaicsAreSortedByFullyQualifiedName() {
		// Arrange:
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();

		// Act:
		attachment.addMosaic(Utils.createMosaicId("b", "c"), Quantity.ZERO);
		attachment.addMosaic(Utils.createMosaicId("a", "b"), Quantity.ZERO);
		attachment.addMosaic(Utils.createMosaicId("b", "a"), Quantity.ZERO);
		attachment.addMosaic(Utils.createMosaicId("aa", "a"), Quantity.ZERO);

		// Assert:
		final Collection<MosaicId> expectedMosaicIds = Arrays.asList(
				Utils.createMosaicId("a", "b"),
				Utils.createMosaicId("aa", "a"),
				Utils.createMosaicId("b", "a"),
				Utils.createMosaicId("b", "c"));
		Assert.assertThat(
				attachment.getMosaics().stream().map(Mosaic::getMosaicId).collect(Collectors.toList()),
				IsEqual.equalTo(expectedMosaicIds));
	}

	//endregion
}