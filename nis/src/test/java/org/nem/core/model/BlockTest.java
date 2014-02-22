package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.KeyPair;
import org.nem.core.test.*;

public class BlockTest {

    //region Fee

    @Test
    public void blockFeeIsInitiallyZero() {
        // Arrange:
        Block block = new Block();

        // Assert:
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(0L));
    }

    @Test
    public void blockFeeIsSumOfTransactionFees() {
        // Arrange:
        Block block = new Block();
        block.addTransaction(createTransactionWithFee(17));
        block.addTransaction(createTransactionWithFee(11));
        block.addTransaction(createTransactionWithFee(22));

        // Assert:
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(50L));
    }

    //endregion

    private static Transaction createTransactionWithFee(final long fee) {
        // Arrange:
        Account sender = new Account(new KeyPair());
        MockTransaction transaction = new MockTransaction(sender);
        transaction.setFee(fee);
        return transaction;
    }
}
