package org.nem.nis.validators.integration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.test.NisUtils;

import java.util.List;

public class MultiBlockBlockChainValidatorTransactionValidationTest extends AbstractBlockChainValidatorTransactionValidationTest {

	@After
	public void afterTest() {
		Utils.resetGlobals();
	}

	@Override
	protected List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions) {
		// put each transaction in a separate block
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, transactions.size());
		for (int i = 0; i < blocks.size(); ++i) {
			blocks.get(i).addTransaction(transactions.get(i));
		}
		return blocks;
	}

	@Override
	protected boolean isSingleBlockUsed() {
		return false;
	}

	@Test
	public void allBlocksInChainMustHaveValidTimestamp() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block = createFutureBlock(blocks.get(2));
		blocks.add(block);

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE));
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInBlockIsSignedByBlockHarvester() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(context.createValidSignedTransaction());
		block.addTransaction(context.createValidSignedTransaction(block.getSigner()));
		block.addTransaction(context.createValidSignedTransaction());
		block.sign();

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
	}

	@Test
	public void mosaicTransfersInLaterBlocksAreAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(),
				BlockMarkerConstants.MOSAICS_FORK(NetworkInfos.getDefault().getVersion() << 24));
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		// - add a namespace creation transaction to block index 0
		final Account mosaicOwner = context.addAccount(Amount.fromNem(200000));
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(mosaicOwner, Utils.createMosaicId("alice", "tokens"),
				Utils.createMosaicProperties(100000L, 1, null, null));
		final ProvisionNamespaceTransaction namespaceTransaction = new ProvisionNamespaceTransaction(TimeInstant.ZERO, mosaicOwner,
				MosaicConstants.NAMESPACE_OWNER_NEM, Amount.fromNem(50000), mosaicDefinition.getId().getNamespaceId().getLastPart(), null);
		blocks.get(0).addTransaction(fixUp(namespaceTransaction));

		// - add a mosaic definition creation transaction to block index 1
		final MosaicDefinitionCreationTransaction creationTransaction = new MosaicDefinitionCreationTransaction(TimeInstant.ZERO,
				mosaicOwner, mosaicDefinition);
		blocks.get(1).addTransaction(fixUp(creationTransaction));

		// - add a mosaic transfer transaction to block index 2
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicDefinition.getId(), new Quantity(1000));
		final Transaction transferTransaction = new TransferTransaction(TimeInstant.ZERO, mosaicOwner, Utils.generateRandomAccount(),
				Amount.fromNem(1), attachment);
		blocks.get(2).addTransaction(fixUp(transferTransaction));
		NisUtils.signAllBlocks(blocks);

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static Transaction fixUp(final Transaction transaction) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));
		transaction.setFee(Amount.fromNem(10000));
		transaction.sign();
		return transaction;
	}
}
