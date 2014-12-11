package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.PoiFacade;
import org.nem.nis.poi.*;
import org.nem.nis.remote.RemoteLink;
import org.nem.nis.test.NisUtils;

public class EligibleSignerBlockValidatorTest {
	private static final int On = ImportanceTransferTransaction.Mode.Activate.value();
	private static final int Off = ImportanceTransferTransaction.Mode.Deactivate.value();

	@Test
	public void accountHarvestingRemotelyCanSignBlockIfRemoteIsNotActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, 1000, ValidationResult.SUCCESS, On);
	}

	@Test
	public void accountHarvestingRemotelyCannotSignBlockIfRemoteIsActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, 1440, ValidationResult.FAILURE_ENTITY_UNUSABLE, On);
	}

	@Test
	public void accountHarvestingRemotelyCannotSignBlockIfRemoteIsNotDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, 1000, ValidationResult.FAILURE_ENTITY_UNUSABLE, Off);
	}

	@Test
	public void accountHarvestingRemotelyCanSignBlockIfRemoteIsDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, 1440, ValidationResult.SUCCESS, Off);
	}

	@Test
	public void accountRemoteHarvesterCannotSignBlockIfRemoteIsNotActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, 1000, ValidationResult.FAILURE_ENTITY_UNUSABLE, On);
	}

	@Test
	public void accountRemoteHarvesterCanSignBlockIfRemoteIsActive() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, 1440, ValidationResult.SUCCESS, On);
	}

	@Test
	public void accountRemoteHarvesterCanSignBlockIfRemoteIsNotDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, 1000, ValidationResult.SUCCESS, Off);
	}

	@Test
	public void accountRemoteHarvesterCannotSignBlockIfRemoteIsDeactivated() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, 1440, ValidationResult.FAILURE_ENTITY_UNUSABLE, Off);
	}

	@Test
	public void accountWithoutRemoteLinkCanSignBlock() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(null, 1000, ValidationResult.SUCCESS, 1);
	}

	private static void assertValidationResultForRemoteLinkOwner(
			final RemoteLink.Owner owner,
			final int blockHeight,
			final ValidationResult expectedResult,
			final int mode) {
		// Arrange:
		final int changeHeight = 5;
		final Block block = NisUtils.createRandomBlockWithHeight(changeHeight + blockHeight);

		final PoiAccountState accountState = new PoiAccountState(block.getSigner().getAddress());
		if (null != owner) {
			accountState.getRemoteLinks().addLink(new RemoteLink(block.getSigner().getAddress(), new BlockHeight(changeHeight), mode, owner));
		}

		final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		Mockito.when(poiFacade.findStateByAddress(block.getSigner().getAddress())).thenReturn(accountState);

		final BlockValidator validator = new EligibleSignerBlockValidator(poiFacade);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}