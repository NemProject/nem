package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;
import org.nem.nis.test.NisUtils;

public class EligibleSignerBlockValidatorTest {

	@Test
	public void accountHarvestingRemotelyCannotSignBlock() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.HarvestingRemotely, ValidationResult.FAILURE_ENTITY_UNUSABLE);
	}

	@Test
	public void accountRemoteHarvesterCanSignBlock() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(RemoteLink.Owner.RemoteHarvester, ValidationResult.SUCCESS);
	}

	@Test
	public void accountWithoutRemoteLinkCanSignBlock() {
		// Assert:
		assertValidationResultForRemoteLinkOwner(null, ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForRemoteLinkOwner(final RemoteLink.Owner owner, final ValidationResult expectedResult) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(5);

		final PoiAccountState accountState = new PoiAccountState(block.getSigner().getAddress());
		if (null != owner) {
			accountState.getRemoteLinks().addLink(new RemoteLink(block.getSigner().getAddress(), new BlockHeight(8), 1, owner));
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