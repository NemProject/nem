package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.BlockValidator;

public class EligibleSignerBlockValidatorTest {
	private static final ImportanceTransferMode ON = ImportanceTransferMode.Activate;
	private static final ImportanceTransferMode OFF = ImportanceTransferMode.Deactivate;

	private static final int VALID_DELAY = NisTestConstants.REMOTE_HARVESTING_DELAY;
	private static final int INVALID_DELAY = VALID_DELAY - 1;

	@Test
	public void blockWithSignerBlockedFromHarvestingIsRejected() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(PublicKey.fromHexString("b74e3914b13cb742dfbceef110d85bad14bd3bb77051a08be93c0f8a0651fde2"));
		final Block block = NisUtils.createRandomBlockWithHeight(new Account(keyPair), 123);
		final BlockValidator validator = new EligibleSignerBlockValidator(new DefaultAccountStateCache());

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CANNOT_HARVEST_FROM_BLOCKED_ACCOUNT));
	}

	@Test
	public void accountHarvestingRemotelyCanSignBlockIfRemoteIsNotActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, INVALID_DELAY, ValidationResult.SUCCESS, ON);
	}

	@Test
	public void accountHarvestingRemotelyCannotSignBlockIfRemoteIsActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, VALID_DELAY,
				ValidationResult.FAILURE_INELIGIBLE_BLOCK_SIGNER, ON);
	}

	@Test
	public void accountHarvestingRemotelyCannotSignBlockIfRemoteIsNotDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, INVALID_DELAY,
				ValidationResult.FAILURE_INELIGIBLE_BLOCK_SIGNER, OFF);
	}

	@Test
	public void accountHarvestingRemotelyCanSignBlockIfRemoteIsDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, VALID_DELAY, ValidationResult.SUCCESS, OFF);
	}

	@Test
	public void accountRemoteHarvesterCannotSignBlockIfRemoteIsNotActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, INVALID_DELAY,
				ValidationResult.FAILURE_INELIGIBLE_BLOCK_SIGNER, ON);
	}

	@Test
	public void accountRemoteHarvesterCanSignBlockIfRemoteIsActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, VALID_DELAY, ValidationResult.SUCCESS, ON);
	}

	@Test
	public void accountRemoteHarvesterCanSignBlockIfRemoteIsNotDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, INVALID_DELAY, ValidationResult.SUCCESS, OFF);
	}

	@Test
	public void accountRemoteHarvesterCannotSignBlockIfRemoteIsDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, VALID_DELAY,
				ValidationResult.FAILURE_INELIGIBLE_BLOCK_SIGNER, OFF);
	}

	@Test
	public void accountWithoutRemoteLinkCanSignBlock() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(null, INVALID_DELAY, ValidationResult.SUCCESS, ON);
	}

	private static void assertValidationResultForRemoteLinkOwner(final RemoteLink.Owner owner, final int blockHeight,
			final ValidationResult expectedResult, final ImportanceTransferMode mode) {
		// Arrange:
		final int changeHeight = 5;
		final Block block = NisUtils.createRandomBlockWithHeight(changeHeight + blockHeight);

		final AccountState accountState = new AccountState(block.getSigner().getAddress());
		if (null != owner) {
			accountState.getRemoteLinks()
					.addLink(new RemoteLink(block.getSigner().getAddress(), new BlockHeight(changeHeight), mode, owner));
		}

		final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		Mockito.when(accountStateCache.findStateByAddress(block.getSigner().getAddress())).thenReturn(accountState);

		final BlockValidator validator = new EligibleSignerBlockValidator(accountStateCache);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
