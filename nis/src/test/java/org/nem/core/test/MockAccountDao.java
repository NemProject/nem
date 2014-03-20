package org.nem.core.test;

import org.nem.core.dao.AccountDao;
import org.nem.core.dbmodel.Account;

import java.util.*;

/**
 * A mock AccountDao implementation.
 */
public class MockAccountDao implements AccountDao {

    private Map<String, Account> knownAccounts = new HashMap<>();

    /**
     * Adds a mapping between a model and db-model account.
     *
     * @param account The model account
     * @param dbAccount The db-model account.
     */
    public void addMapping(final org.nem.core.model.Account account, final org.nem.core.dbmodel.Account dbAccount) {
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
    public void saveMulti(List<Account> recipientsAccounts) {
        throw new UnsupportedOperationException();
    }
}