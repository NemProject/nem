package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class AccountTest {

    @Test
    public void ctorInitializesAllFields() {
        // Arrange:
        final KeyPair kp = new KeyPair();
        final Address expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
        final Account account = new Account(kp);

        // Assert:
        Assert.assertThat(account.getKeyPair(), IsEqual.equalTo(kp));
        Assert.assertThat(account.getPublicKey(), IsEqual.equalTo(kp.getPublicKey()));
        Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(0L));
        Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
        Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
    }

    @Test
    public void labelCanBeSet() {
        // Arrange:
        final Account account = Utils.generateRandomAccount();

        // Act:
        account.setLabel("Beta Gamma");

        // Assert:
        Assert.assertThat(account.getLabel(), IsEqual.equalTo("Beta Gamma"));
    }

    @Test
    public void balanceCanBeIncremented() {
        // Arrange:
        final Account account = Utils.generateRandomAccount();

        // Act:
        account.incrementBalance(7);
        account.incrementBalance(12);

        // Assert:
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(19L));
    }

    @Test
    public void balanceCanBeIncrementedMultipleTimes() {
        // Arrange:
        final Account account = Utils.generateRandomAccount();

        // Act:
        account.incrementBalance(7);

        // Assert:
        Assert.assertThat(account.getBalance(), IsEqual.equalTo(7L));
    }

    @Test
    public void encryptedMessagesCanBeDecrypted() {
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
        Assert.assertThat(recipientAccount.getMessages().get(0).getEncryptedMessage(), IsEqual.equalTo(encryptedInput));
        Assert.assertThat(recipientAccount.getMessages().get(0).getDecryptedMessage(), IsEqual.equalTo(input));
    }

    @Test
    public void multipleEncryptedMessagesCanBeDecrypted() {
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
        Assert.assertThat(recipientAccount.getMessages().get(0).getEncryptedMessage(), IsEqual.equalTo(encryptedInput1));
        Assert.assertThat(recipientAccount.getMessages().get(0).getDecryptedMessage(), IsEqual.equalTo(input1));
        Assert.assertThat(recipientAccount.getMessages().get(1).getEncryptedMessage(), IsEqual.equalTo(encryptedInput2));
        Assert.assertThat(recipientAccount.getMessages().get(1).getDecryptedMessage(), IsEqual.equalTo(input2));
    }

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        KeyPair kp = new KeyPair();
        Account account = new Account(kp);

        // Assert:
        for (final Account account2 : createEquivalentAccounts(kp))
            Assert.assertThat(account2, IsEqual.equalTo(account));

        for (final Account account2 : createNonEquivalentAccounts(kp))
            Assert.assertThat(account2, IsNot.not(IsEqual.equalTo(account)));

        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(account)));
        Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)account)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        KeyPair kp = new KeyPair();
        Account account = new Account(kp);
        int hashCode = account.hashCode();

        // Assert:
        for (final Account account2 : createEquivalentAccounts(kp))
            Assert.assertThat(account2.hashCode(), IsEqual.equalTo(hashCode));

        for (final Account account2 : createNonEquivalentAccounts(kp))
            Assert.assertThat(account2.hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
    }

    private static Account[] createEquivalentAccounts(final KeyPair keyPair) {
        return new Account[] {
            new Account(keyPair),
            new Account(new KeyPair(keyPair.getPublicKey())),
            new Account(new KeyPair(keyPair.getPrivateKey()))
        };
    }

    private static Account[] createNonEquivalentAccounts(final KeyPair keyPair) {
        return new Account[] {
            Utils.generateRandomAccount(),
            new Account(new KeyPair(Utils.incrementAtIndex(keyPair.getPublicKey(), 10))),
            new Account(new KeyPair(keyPair.getPrivateKey().add(new BigInteger("1"))))
        };
    }

    //endregion
}
