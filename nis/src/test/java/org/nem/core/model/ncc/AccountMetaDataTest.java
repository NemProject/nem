package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockAmount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import java.util.Arrays;
import java.util.List;

public class AccountMetaDataTest {

	@Test
	public void canCreateAccountMetaData() {
		// Arrange:
		final TestContext context = new TestContext(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE);
		final AccountMetaData metaData = context.createAccountMetaData();

		// Assert:
		context.assertContext(metaData);
	}

	@Test
	public void canRoundTripAccountMetaData() {
		// Arrange:
		final TestContext context = new TestContext(AccountStatus.LOCKED, AccountRemoteStatus.DEACTIVATING);
		final AccountMetaData metaData = context.createRoundTrippedAccountMetaData(true);

		// Assert:
		context.assertContext(metaData);
	}

	@Test
	public void canRoundTripAccountWithEmptyCosignatories() {
		// Arrange:
		final TestContext context = new TestContext(AccountStatus.LOCKED, AccountRemoteStatus.DEACTIVATING, Arrays.asList());
		final AccountMetaData metaData = context.createRoundTrippedAccountMetaData(false);

		// Assert:
		context.assertContext(metaData);
	}

	public static class TestContext {
		final AccountStatus status;
		final AccountRemoteStatus remoteStatus;
		final private List<AccountInfo> cosignatoryOf;

		TestContext(final AccountStatus status, final AccountRemoteStatus remoteStatus) {
			this(status, remoteStatus, Arrays.asList(
					new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(123), new BlockAmount(234), "account1", 0.1),
					new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(345), new BlockAmount(456), "account2", 0.2)
			));
		}

		public TestContext(final AccountStatus status, final AccountRemoteStatus remoteStatus, final List<AccountInfo> accountInfoList) {
			this.status = status;
			this.remoteStatus = remoteStatus;
			this.cosignatoryOf = accountInfoList;
		}

		public AccountMetaData createAccountMetaData() {
			return new AccountMetaData(this.status, this.remoteStatus, this.cosignatoryOf);
		}

		private AccountMetaData createRoundTrippedAccountMetaData(boolean b) {
			// Arrange:
			final AccountMetaData metaData = this.createAccountMetaData();

			// Act:
			final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
			return new AccountMetaData(deserializer);
		}

		public void assertContext(final AccountMetaData metaData) {
			Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(this.status));
			Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(this.remoteStatus));

			Assert.assertThat(metaData.getCosignatoryOf().size(), IsEqual.equalTo(this.cosignatoryOf.size()));
			for (int i = 0; i < this.cosignatoryOf.size(); ++i) {
				assertAccountInfo(metaData.getCosignatoryOf().get(i), this.cosignatoryOf.get(i));
			}
		}

		private static void assertAccountInfo(final AccountInfo actual, final AccountInfo expected) {
			Assert.assertThat(actual.getAddress(), IsEqual.equalTo(expected.getAddress()));
			Assert.assertThat(actual.getBalance(), IsEqual.equalTo(expected.getBalance()));
			Assert.assertThat(actual.getNumHarvestedBlocks(), IsEqual.equalTo(expected.getNumHarvestedBlocks()));
			Assert.assertThat(actual.getLabel(), IsEqual.equalTo(expected.getLabel()));
			Assert.assertThat(actual.getImportance(), IsEqual.equalTo(expected.getImportance()));
		}
	}
}
