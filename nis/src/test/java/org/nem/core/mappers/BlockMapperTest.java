package org.nem.core.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.dao.AccountDao;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;

import java.util.*;

public class BlockMapperTest {

    @Test
    public void blockModelWithoutTransactionsCanBeMappedToDbModel() {
        final Block model = new Block(
            Utils.generateRandomAccount(),
            Utils.generateRandomBytes(),
            new TimeInstant(721),
            17);

        model.sign();

        final MockAccountDao accountDao = new MockAccountDao();
        final org.nem.core.dbmodel.Account dbSigner = new org.nem.core.dbmodel.Account();
        accountDao.setMapping(model.getSigner(), dbSigner);

        final byte[] hash = HashUtils.calculateHash(model);

        // Act:
        final org.nem.core.dbmodel.Block dbModel = BlockMapper.toDbModel(model, accountDao);

        // Assert:
        Assert.assertThat(dbModel.getId(), IsEqual.equalTo(null));
        Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(ByteUtils.bytesToLong(hash)));
        Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(model.getPreviousBlockHash()));
        Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(hash));
        Assert.assertThat(dbModel.getTimestamp(), IsEqual.equalTo(721));
        Assert.assertThat(dbModel.getForger(), IsEqual.equalTo(dbSigner));
        Assert.assertThat(dbModel.getForgerProof(), IsEqual.equalTo(model.getSignature().getBytes()));
        Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(17L));
        Assert.assertThat(dbModel.getTotalAmount(), IsEqual.equalTo(0L));
        Assert.assertThat(dbModel.getTotalFee(), IsEqual.equalTo(0L));
        Assert.assertThat(dbModel.getNextBlockId(), IsEqual.equalTo(null));
        Assert.assertThat(dbModel.getBlockTransfers().size(), IsEqual.equalTo(0));
    }

    // TODO: finish tests
//    @Test
//    public void transferModelWithMessageCanBeMappedToDbModel() {
//    }

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
