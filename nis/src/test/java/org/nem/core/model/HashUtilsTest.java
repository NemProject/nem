package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.MockVerifiableEntity;
import org.nem.core.test.Utils;

public class HashUtilsTest {

    @Test
    public void identicalEntitiesHaveSameHash() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);

        // Act:
        byte[] hash1 = HashUtils.calculateHash(entity1);
        byte[] hash2 = HashUtils.calculateHash(entity2);

        // Assert:
        Assert.assertThat(hash1, IsEqual.equalTo(hash2));
    }

    @Test
    public void differentEntitiesHaveDifferentHashes() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 8);

        // Act:
        byte[] hash1 = HashUtils.calculateHash(entity1);
        byte[] hash2 = HashUtils.calculateHash(entity2);

        // Assert:
        Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
    }

    @Test
    public void signatureDoesNotChangeEntity() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);
        entity2.sign();

        // Act:
        byte[] hash1 = HashUtils.calculateHash(entity1);
        byte[] hash2 = HashUtils.calculateHash(entity2);

        // Assert:
        Assert.assertThat(hash1, IsEqual.equalTo(hash2));
    }

    @Test
    public void changingPayloadChangesHash() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        byte[] hash1 = HashUtils.calculateHash(entity);

        // Act:
        entity.setCustomField(6);
        byte[] hash2 = HashUtils.calculateHash(entity);

        // Assert:
        Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
    }

    //endregion
}
