package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.time.TimeInstant;

public class GenesisBlockTest {

    private final static String GENESIS_ACCOUNT = "NBERUJIKSAPW54YISFOJZ2PLG3E7CACCNN2Z6SOW";

    @Test
    public void genesisBlockCanBeCreated() {
        // Act:
        final Block block = new GenesisBlock(new TimeInstant(12));

        // Assert:
        Assert.assertThat(block.getSigner().getAddress().getEncoded(), IsEqual.equalTo(GENESIS_ACCOUNT));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(12)));

//        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(0L)); TODO: verify
        Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(new byte[32]));
        Assert.assertThat(block.getHeight(), IsEqual.equalTo(1L));
//        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0)); TODO: verify
    }

    @Test
    public void genesisBlockIsVerifiable() {
        // Arrange:
        final Block block = new GenesisBlock(TimeInstant.ZERO);

        // Assert:
        Assert.assertThat(block.verify(), IsEqual.equalTo(true));
    }

    @Test
    public void genesisTransactionsAreVerifiable() {
        // Arrange:
        final Block block = new GenesisBlock(TimeInstant.ZERO);

        // Assert:
        for (final Transaction transaction : block.getTransactions())
            Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
    }
}
