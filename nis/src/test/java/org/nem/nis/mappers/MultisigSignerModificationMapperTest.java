package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.MultisigSignerModification;
import org.nem.nis.test.MockAccountDao;

import java.util.Arrays;
import java.util.List;

public class MultisigSignerModificationMapperTest {

	@Test
	public void multisigSignerModificationModelWithModificationTypeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(MultisigModificationType.Add, 123L);

		// Act:
		final MultisigSignerModification dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}

	@Test
	public void multisigSignerModificationModelWithHighFeeCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext(MultisigModificationType.Add, 12345L);

		// Act:
		final MultisigSignerModification dbModel = context.toDbModel(7);

		// Assert:
		context.assertDbModel(dbModel, 7);
	}


	@Test
	public void importanceTransferModelCanBeRoundTripped() {
		// Arrange:
		final TestContext context = new TestContext(MultisigModificationType.Add, 123L);
		final MultisigSignerModification dbModel = context.toDbModel(7);

		// Act:
		final MultisigSignerModificationTransaction model = context.toModel(dbModel);

		// Assert:
		context.assertModel(model);
	}
	private class TestContext {
		private final MultisigSignerModificationTransaction model;
		private final org.nem.nis.dbmodel.Account dbSender;
		private final org.nem.nis.dbmodel.Account dbCosignatory;
		private final MockAccountDao accountDao;
		private final Hash hash;
		private final long fee;

		public TestContext(final MultisigModificationType modificationType, final Account sender, final Account cosignatoryAccount, final long fee) {
			final List<MultisigModification> modifications = Arrays.asList(new MultisigModification(modificationType, cosignatoryAccount));
			this.model = new MultisigSignerModificationTransaction(new TimeInstant(721), sender, modifications);

			this.fee = fee;
			this.model.setFee(Amount.fromNem(fee));
			this.model.setDeadline(new TimeInstant(800));
			this.model.sign();

			this.dbSender = new org.nem.nis.dbmodel.Account();
			this.dbSender.setPrintableKey(this.model.getSigner().getAddress().getEncoded());
			this.dbSender.setPublicKey(this.model.getSigner().getKeyPair().getPublicKey());

			this.dbCosignatory = new org.nem.nis.dbmodel.Account();
			this.dbCosignatory.setPrintableKey(cosignatoryAccount.getAddress().getEncoded());
			this.dbCosignatory.setPublicKey(cosignatoryAccount.getKeyPair().getPublicKey());

			this.accountDao = new MockAccountDao();
			this.accountDao.addMapping(this.model.getSigner(), this.dbSender);
			this.accountDao.addMapping(cosignatoryAccount, this.dbCosignatory);

			this.hash = HashUtils.calculateHash(this.model);
		}

		public TestContext(final MultisigModificationType modificationType, final long fee) {
			this(modificationType, Utils.generateRandomAccount(), Utils.generateRandomAccount(), fee);
		}

		public MultisigSignerModificationTransaction getModel() {
			return this.model;
		}

		public MultisigSignerModification toDbModel(final int blockIndex) {
			return MultisigSignerModificationMapper.toDbModel(this.model, blockIndex, 0, new AccountDaoLookupAdapter(this.accountDao));
		}

		public MultisigSignerModificationTransaction toModel(final MultisigSignerModification dbTransfer) {
			final MockAccountLookup mockAccountLookup = new MockAccountLookup();
			mockAccountLookup.setMockAccount(this.model.getSigner());
			mockAccountLookup.setMockAccount(this.model.getModifications().get(0).getCosignatory());
			return MultisigSignerModificationMapper.toModel(dbTransfer, mockAccountLookup);
		}

		public void assertDbModel(final MultisigSignerModification dbModel, final int blockIndex) {
			Assert.assertThat(dbModel.getId(), IsNull.nullValue());
			Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(this.hash.getShortId()));
			Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(this.hash));
			Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(Amount.fromNem(Math.max(1000L, this.fee)).getNumMicroNem()));
			Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));
			Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
			Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(this.dbSender));
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(this.model.getSignature().getBytes()));
			Assert.assertThat(dbModel.getCosignatory(), IsEqual.equalTo(this.dbCosignatory));
			final MultisigModification expectedModification = this.model.getModifications().get(0);
			Assert.assertThat(dbModel.getModificationType(), IsEqual.equalTo(expectedModification.getModificationType().value()));
			Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(blockIndex));
			Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
			Assert.assertThat(dbModel.getBlock(), IsNull.nullValue());

			final PublicKey signerPublicKey = this.model.getSigner().getKeyPair().getPublicKey();
			Assert.assertThat(dbModel.getSender().getPublicKey(), IsEqual.equalTo(signerPublicKey));
			final PublicKey remotePublicKey = this.model.getModifications().get(0).getCosignatory().getAddress().getPublicKey();
			Assert.assertThat(dbModel.getCosignatory().getPublicKey(), IsEqual.equalTo(remotePublicKey));
		}

		public void assertModel(final MultisigSignerModificationTransaction rhs) {
			Assert.assertThat(HashUtils.calculateHash(rhs), IsEqual.equalTo(this.hash));
			Assert.assertThat(rhs.getVersion(), IsEqual.equalTo(this.model.getVersion()));
			Assert.assertThat(rhs.getType(), IsEqual.equalTo(this.model.getType()));
			Assert.assertThat(rhs.getFee(), IsEqual.equalTo(this.model.getFee()));
			Assert.assertThat(rhs.getTimeStamp(), IsEqual.equalTo(this.model.getTimeStamp()));
			Assert.assertThat(rhs.getDeadline(), IsEqual.equalTo(this.model.getDeadline()));
			Assert.assertThat(rhs.getSigner(), IsEqual.equalTo(this.model.getSigner()));
			Assert.assertThat(rhs.getSignature(), IsEqual.equalTo(this.model.getSignature()));
			Assert.assertThat(rhs.getModifications().size(), IsEqual.equalTo(this.model.getModifications().size()));
			final MultisigModification rhsModification = rhs.getModifications().get(0);
			final MultisigModification expectedModification = this.model.getModifications().get(0);
			Assert.assertThat(rhsModification.getCosignatory(), IsEqual.equalTo(expectedModification.getCosignatory()));
			Assert.assertThat(rhsModification.getCosignatory().getAddress().getPublicKey(), IsEqual.equalTo(expectedModification.getCosignatory().getAddress().getPublicKey()));
			Assert.assertThat(rhsModification.getModificationType(), IsEqual.equalTo(expectedModification.getModificationType()));
		}
	}
}
