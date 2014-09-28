package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.test.MockAccountDao;

import java.util.List;

public class ImportanceTransferMapperTest {

	@Test
	public void importanceTransferModelWithDirectionTransferCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransaction.Mode.Activate);

		// Act:
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void importanceTransferModelWithDirectionRevertCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransaction.Mode.Deactivate);

		// Act:
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void importanceTransferModelCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransaction.Mode.Deactivate);
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Act:
		final ImportanceTransferTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}

	// TODO 20140923 J-G not sure if this test is valid because i think the ImportanceTransferTransaction ctor is throwing (so the failure is outside of the mapper layer) (this is hidden because of expected vs ExceptionAssert)
	// TODO 20140924 G-J I wanted to be sure, that if in db, there will be some value different than those 2 values, that are mapped (Activate/Deactivate),
	// calling toModel will fail (this is if we would like to add some other modes in future, but I guess this test in this place does not have any sense)
	@Test(expected = IllegalArgumentException.class)
	public void importanceTransferDbModelWithUnknownDirectionTypeCannotBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext(ImportanceTransferTransaction.Mode.Unknown);
		final ImportanceTransfer dbModel = context.toDbModel(7);

		// Act:
		context.toModel(dbModel);
	}

	private class TestContext {

		private final ImportanceTransferTransaction model;
		private final org.nem.nis.dbmodel.Account dbSender;
		private final org.nem.nis.dbmodel.Account dbRemote;
		private final MockAccountDao accountDao;
		private final Hash hash;

		public TestContext(final ImportanceTransferTransaction.Mode mode, final Account sender, final Account remote) {
			this.model = new ImportanceTransferTransaction(
					new TimeInstant(721),
					sender,
					mode,
					remote);

			this.model.setFee(Amount.fromNem(11));
			this.model.setDeadline(new TimeInstant(800));
			this.model.sign();

			this.dbSender = new org.nem.nis.dbmodel.Account();
			this.dbSender.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbSender.setPublicKey(this.model.getSigner().getKeyPair().getPublicKey());

			this.dbRemote = new org.nem.nis.dbmodel.Account();
			this.dbRemote.setPrintableKey(this.model.getRemote().getAddress().getEncoded());
			this.dbRemote.setPublicKey(remote.getKeyPair().getPublicKey());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbSender);
			this.accountDao.addMapping(this.model.getRemote(), this.dbRemote);

			this.hash = HashUtils.calculateHash(this.model);
		}

		public TestContext(final ImportanceTransferTransaction.Mode mode) {
			this(mode, Utils.generateRandomAccount(), Utils.generateRandomAccount());
		}

		public ImportanceTransferTransaction getModel() {
			return this.model;
		}

		@SuppressWarnings("unchecked")
		public ImportanceTransfer toDbModel(final int blockIndex) {
			final ImportanceTransfer ret = ImportanceTransferMapper.toDbModel(this.model, blockIndex, new AccountDaoLookupAdapter(this.accountDao));

			// TODO 20140923 J-G i'm not following what you're doing here
			// TODO 20140924 G-J: this was made to make .getBlock and .getBlkIndex inside asserts
			// work, but I guess that doesn't make much sense, and it probably would be better to simply
			// remove those asserts
			// hackery
			final org.nem.nis.dbmodel.Block b = Mockito.mock(org.nem.nis.dbmodel.Block.class);
			final List<ImportanceTransfer> l = (List<ImportanceTransfer>)Mockito.mock(List.class);
			Mockito.when(l.indexOf(ret)).thenReturn(blockIndex);
			Mockito.when(b.getBlockImportanceTransfers()).thenReturn(l);
			ret.setBlock(b);

			return ret;
		}

		public ImportanceTransferTransaction toModel(final ImportanceTransfer dbTransfer) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			mockAccountLookup.setMockAccount(this.model.getRemote());
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
			// TODO 20140923 should the db method be called getMode too?
			Assert.assertThat(dbModel.getDirection(), IsEqual.equalTo(this.model.getMode().value()));
			Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(blockIndex));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
			// TODO 20140923 why is the block not null here (but null for transfer)?
			Assert.assertThat(dbModel.getBlock(), IsNull.notNullValue());

			final PublicKey signerPublicKey = this.model.getSigner().getKeyPair().getPublicKey();
			Assert.assertThat(dbModel.getSender().getPublicKey(), IsEqual.equalTo(signerPublicKey));
			final PublicKey remotePublicKey = this.model.getRemote().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getRemote().getPublicKey(), IsEqual.equalTo(remotePublicKey));
		}

		public void assertModel(final ImportanceTransferTransaction rhs) {
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getFee(), IsEqual.equalTo(this.model.getFee()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getDeadline(), IsEqual.equalTo(this.model.getDeadline()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));
			Assert.assertThat(rhs.getRemote(), IsEqual.equalTo(this.model.getRemote()));
			Assert.assertThat(rhs.getRemote().getAddress().getPublicKey(), IsEqual.equalTo(this.model.getRemote().getAddress().getPublicKey()));
			Assert.assertThat(rhs.getMode(), IsEqual.equalTo(this.model.getMode()));
		}
	}
}