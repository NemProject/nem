package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Cipher;
import org.nem.core.crypto.KeyPair;
import org.nem.core.test.Utils;

public class AccountTest {

    @Test
    public void ctorInitializesAllFields() throws Exception {
        // Arrange:
        final KeyPair kp = new KeyPair();
        final String expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
        final Account account = new Account(kp);

        // Assert:
        Assert.assertThat(account.getPublicKey(), IsEqual.equalTo(kp.getPublicKey()));
        Assert.assertThat(account.getId(), IsEqual.equalTo(expectedAccountId));
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(0L));
        Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
        Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
    }

    @Test
    public void labelCanBeSet() throws Exception {
        // Arrange:
        final Account account = new Account(new KeyPair());

        // Act:
        account.setLabel("Beta Gamma");

        // Assert:
        Assert.assertThat(account.getLabel(), IsEqual.equalTo("Beta Gamma"));
    }

    @Test
    public void balanceCanBeIncremented() throws Exception {
        // Arrange:
        final Account account = new Account(new KeyPair());

        // Act:
        account.incrementBalance(7);
        account.incrementBalance(12);

        // Assert:
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(19L));
    }

    @Test
    public void balanceCanBeIncrementedMultipleTimes() throws Exception {
        // Arrange:
        final Account account = new Account(new KeyPair());

        // Act:
        account.incrementBalance(7);

        // Assert:
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(7L));
    }

    @Test
    public void encryptedMessagesCanBeDecrypted() throws Exception {
        // Arrange:
        final KeyPair skp = new KeyPair();
        final KeyPair rkp = new KeyPair();
        final Cipher cipher = new Cipher(skp, rkp);
        final byte[] input = Utils.generateRandomBytes();
        final byte[] encryptedInput = cipher.encrypt(input);
        final Account senderAccount = new Account(new KeyPair(skp.getPublicKey()));
        final Account recipientAccount = new Account(rkp);

        // Act:
        recipientAccount.addMessage(senderAccount, encryptedInput);

        // Assert:
        Assert.assertThat(recipientAccount.getMessages().size(), IsEqual.equalTo(1));
        Assert.assertThat(recipientAccount.getMessages().get(0), IsEqual.equalTo(input));
    }

    @Test
    public void multipleEncryptedMessagesCanBeDecrypted() throws Exception {
        // Arrange:
        final KeyPair skp = new KeyPair();
        final KeyPair rkp = new KeyPair();
        final Cipher cipher = new Cipher(skp, rkp);
        final byte[] input1 = Utils.generateRandomBytes();
        final byte[] input2 = Utils.generateRandomBytes();
        final byte[] encryptedInput1 = cipher.encrypt(input1);
        final byte[] encryptedInput2 = cipher.encrypt(input2);
        final Account senderAccount = new Account(new KeyPair(skp.getPublicKey()));
        final Account recipientAccount = new Account(rkp);

        // Act:
        recipientAccount.addMessage(senderAccount, encryptedInput1);
        recipientAccount.addMessage(senderAccount, encryptedInput2);

        // Assert:
        Assert.assertThat(recipientAccount.getMessages().size(), IsEqual.equalTo(2));
        Assert.assertThat(recipientAccount.getMessages().get(0), IsEqual.equalTo(input1));
        Assert.assertThat(recipientAccount.getMessages().get(1), IsEqual.equalTo(input2));
    }
}
