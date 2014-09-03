package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.MockAccountDao;

import java.util.Arrays;
import java.util.List;

public class ImportanceTransferMapperTest {

	@Test
	public void importanceTransferModelWithDirectionTransferCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransactionDirection.Transfer);

		// Act:
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void importanceTransferModelWithDirectionRevertCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransactionDirection.Revert);

		// Act:
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void  importanceTransferModelCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransactionDirection.Revert);
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Act:
		final ImportanceTransferTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	@Test(expected = IllegalArgumentException.class)
	public void importanceTransferDbModelWithUnknownDirectionTypeCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext(666);
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Act:
		context.toModel(dbModel);
	}

	private class TestContext {

		private final ImportanceTransferTransaction model;
		private final org.nem.nis.dbmodel.Account dbSender;
		private final org.nem.nis.dbmodel.Account dbRemote;
		private final Account remote;
		private final MockAccountDao accountDao;
		private final Hash hash;
		private final Integer direction;

		public TestContext(final int direction, final Account sender, final Account remote) {
			this.model = new ImportanceTransferTransaction(
					new TimeInstant(721),
					sender,
					direction,
					remote);

			this.model.setFee(Amount.fromNem(11));
			this.model.setDeadline(new TimeInstant(800));
			this.model.sign();

			this.remote = remote;

			this.direction = this.model.getDirection();

			this.dbSender = new org.nem.nis.dbmodel.Account();
			this.dbSender.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbSender.setPublicKey(this.model.getSigner().getKeyPair().getPublicKey());

			this.dbRemote = new org.nem.nis.dbmodel.Account();
			this.dbRemote.setPrintableKey(this.model.getRemote().getAddress().getEncoded());
			this.dbRemote.setPublicKey(this.remote.getKeyPair().getPublicKey());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbSender);
			this.accountDao.addMapping(this.model.getRemote(), this.dbRemote);

			this.hash = HashUtils.calculateHash(this.model);
		}

		public TestContext(final int direction) {
			this(direction, Utils.generateRandomAccount(), Utils.generateRandomAccount());
		}

		public ImportanceTransferTransaction getModel() {
			return this.model;
		}

		public ImportanceTransfer toDbModel(final int blockIndex) {
			final ImportanceTransfer ret = ImportanceTransferMapper.toDbModel(this.model, blockIndex, new AccountDaoLookupAdapter(this.accountDao));

			// hackery
			final org.nem.nis.dbmodel.Block b = Mockito.mock(org.nem.nis.dbmodel.Block.class);
			final List<ImportanceTransfer> l = (List<ImportanceTransfer>)Mockito.mock(List.class);
			Mockito.when(l.indexOf(ret)).thenReturn(new Integer(blockIndex));
			Mockito.when(b.getBlockImportanceTransfers()).thenReturn(l);
			ret.setBlock(b);

			return ret;
		}

		public ImportanceTransferTransaction toModel(final ImportanceTransfer dbTransfer) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			mockAccountLookup.setMockAccount(this.remote);
			return ImportanceTransferMapper.toModel(dbTransfer, mockAccountLookup);
		}

		public void assertDbModel(final ImportanceTransfer dbModel, final int blockIndex) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(this.hash.getShortId()));
			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getType(), IsEqual.equalTo(TransactionTypes.IMPORTANCE_TRANSFER));
			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(Amount.fromNem(11L).getNumMicroNem()));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));
			Assert.assertThat(dbModel.getRemote(), IsEqual.equalTo(this.dbRemote));
			Assert.assertThat(dbModel.getDirection(), IsEqual.equalTo(this.direction));
			Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(blockIndex));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
			//Assert.assertThat(dbModel.getBlock(), IsNull.nullValue());

			final PublicKey signerPublicKey = this.model.getSigner().getKeyPair().getPublicKey();
			Assert.assertThat(dbModel.getSender().getPublicKey(), IsEqual.equalTo(signerPublicKey));
			final PublicKey remotePublicKey = this.model.getRemote().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getRemote().getPublicKey(), IsEqual.equalTo(remotePublicKey));
		}

		public void assertModel(final ImportanceTransferTransaction rhs) {
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getFee(), IsEqual.equalTo(this.model.getFee()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getDeadline(), IsEqual.equalTo(this.model.getDeadline()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));
			Assert.assertThat(rhs.getRemote(), IsEqual.equalTo(this.model.getRemote()));
			Assert.assertThat(rhs.getRemote().getAddress().getPublicKey(), IsEqual.equalTo(this.model.getRemote().getAddress().getPublicKey()));
			Assert.assertThat(rhs.getDirection(), IsEqual.equalTo(this.model.getDirection()));
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
		}
	}
}