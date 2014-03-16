package org.nem.core.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.dao.AccountDao;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.test.MockAccountAnalyzer;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;

import java.util.*;

public class TransferMapperTest {

    @Test
    public void transferModelWithoutMessageCanBeMappedToDbModel() {
        final TransferTransaction model = new TransferTransaction(
            new TimeInstant(721),
            Utils.generateRandomAccount(),
            Utils.generateRandomAccount(),
            new Amount(144),
            null);

        model.setFee(new Amount(11));
        model.setDeadline(new TimeInstant(800));
        model.sign();

        final MockAccountDao accountDao = new MockAccountDao();
        final org.nem.core.dbmodel.Account dbSender = new org.nem.core.dbmodel.Account();
        final org.nem.core.dbmodel.Account dbRecipient = new org.nem.core.dbmodel.Account();
        accountDao.setMapping(model.getSigner(), dbSender);
        accountDao.setMapping(model.getRecipient(), dbRecipient);

        final byte[] hash = HashUtils.calculateHash(model);

        // Act:
        final Transfer dbModel = TransferMapper.toDbModel(model, 7, accountDao);

        // Assert:
        Assert.assertThat(dbModel.getId(), IsEqual.equalTo(null));
        Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(ByteUtils.bytesToLong(hash)));
        Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(hash));
        Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(dbModel.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
        Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(11L));
        Assert.assertThat(dbModel.getTimestamp(), IsEqual.equalTo(721));
        Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
        Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbSender));
        Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(model.getSignature().getBytes()));
        Assert.assertThat(dbModel.getRecipient(), IsEqual.equalTo(dbRecipient));
        Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(7));
        Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(144L));
        Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
        Assert.assertThat(dbModel.getBlock(), IsEqual.equalTo(null));
    }

    // TODO: finish tests

//    @Test
//    public void transferModelWithMessageCanBeMappedToDbModel() {
//    }

	@Test
	public void transferModelWithoutMessageCanBeRoundTripped() {
		// Arrange:
		Account siger = Utils.generateRandomAccount();
		Account recipient = Utils.generateRandomAccount();
		final TransferTransaction model = new TransferTransaction(
				new TimeInstant(721),
				siger,
				recipient,
				new Amount(144),
				null);

		model.setFee(new Amount(11));
		model.setDeadline(new TimeInstant(800));
		model.sign();

		final MockAccountDao accountDao = new MockAccountDao();
		final org.nem.core.dbmodel.Account dbSender = new org.nem.core.dbmodel.Account();
		final org.nem.core.dbmodel.Account dbRecipient = new org.nem.core.dbmodel.Account();
		dbSender.setPublicKey(siger.getKeyPair().getPublicKey());
		dbSender.setPrintableKey(siger.getAddress().getEncoded());
		dbRecipient.setPrintableKey(recipient.getAddress().getEncoded());
		// no need for recipients PUBKEY

		accountDao.setMapping(model.getSigner(), dbSender);
		accountDao.setMapping(model.getRecipient(), dbRecipient);

		final byte[] hash = HashUtils.calculateHash(model);

		final MockAccountLookup mockAccountLookup = new MockAccountLookup();
		mockAccountLookup.setMockAccount(model.getSigner());
		mockAccountLookup.setMockAccount(model.getRecipient());

		// Act:
		final Transfer dbModel = TransferMapper.toDbModel(model, 7, accountDao);
		final TransferTransaction resModel = TransferMapper.toModel(dbModel, mockAccountLookup);

		// Assert:
		Assert.assertThat(HashUtils.calculateHash(resModel), IsEqual.equalTo(hash));
		Assert.assertThat(resModel.getVersion(), IsEqual.equalTo(model.getVersion()));
		Assert.assertThat(resModel.getType(), IsEqual.equalTo(model.getType()));
		Assert.assertThat(resModel.getFee(), IsEqual.equalTo(model.getFee()));
		Assert.assertThat(resModel.getTimeStamp(), IsEqual.equalTo(model.getTimeStamp()));
		Assert.assertThat(resModel.getDeadline(), IsEqual.equalTo(model.getDeadline()));
		Assert.assertThat(resModel.getSigner(), IsEqual.equalTo(model.getSigner()));
		Assert.assertThat(resModel.getSignature(), IsEqual.equalTo(model.getSignature()));
		Assert.assertThat(resModel.getRecipient(), IsEqual.equalTo(model.getRecipient()));

		Assert.assertThat(resModel.getAmount(), IsEqual.equalTo(model.getAmount()));
	}

    private class MockAccountDao implements AccountDao {

        private Map<String, org.nem.core.dbmodel.Account> knownAccounts = new HashMap<>();

        public void setMapping(final Account account, final org.nem.core.dbmodel.Account dbAccount) {
            this.knownAccounts.put(account.getAddress().getEncoded(), dbAccount);
        }

        @Override
        public org.nem.core.dbmodel.Account getAccount(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.nem.core.dbmodel.Account getAccountByPrintableAddress(final String printableAddress) {
            return this.knownAccounts.get(printableAddress);
        }

        @Override
        public void save(org.nem.core.dbmodel.Account account) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveMulti(List<org.nem.core.dbmodel.Account> recipientsAccounts) {
            throw new UnsupportedOperationException();
        }
    }
}
