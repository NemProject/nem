package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.test.*;

public class RemoteLinksTest {

	// region constructor

	@Test
	public void remotesAreNotSetUpByDefault() {
		// Arrange:
		final ReadOnlyRemoteLinks links = new RemoteLinks();

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	// endregion

	// region addLink

	@Test
	public void canAddLinkMarkingAccountAsHarvestingRemotely() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Act:
		final RemoteLink link = RemoteLinkFactory.activateHarvestingRemotely(Utils.generateRandomAddress(), new BlockHeight(7));
		links.addLink(link);

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.getCurrent(), IsEqual.equalTo(link));
	}

	@Test
	public void canAddLinkMarkingAccountAsRemoteHarvester() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Act:
		final RemoteLink link = RemoteLinkFactory.activateRemoteHarvester(Utils.generateRandomAddress(), new BlockHeight(7));
		links.addLink(link);

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(links.getCurrent(), IsEqual.equalTo(link));
	}

	// endregion

	// region removeLink

	@Test
	public void cannotRemoveLinkWhenNoLinksArePresent() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Act:
		ExceptionAssert.assertThrows(v -> links.removeLink(createRandomLink()), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemoveInconsistentLink() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();
		links.addLink(createRandomLink());

		// Act:
		ExceptionAssert.assertThrows(v -> links.removeLink(createRandomLink()), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveConsistentLink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final RemoteLinks links = new RemoteLinks();

		final RemoteLink link1 = RemoteLinkFactory.activateRemoteHarvester(address, new BlockHeight(7));
		links.addLink(link1);

		// Act:
		final RemoteLink link2 = RemoteLinkFactory.activateRemoteHarvester(address, new BlockHeight(7));
		links.removeLink(link2);

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	// endregion

	// region storage

	@Test
	public void canStoreMaximumOfTwoLinks() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		final RemoteLink link1 = createRandomLink();
		final RemoteLink link2 = createRandomLink();
		final RemoteLink link3 = createRandomLink();
		links.addLink(link1);
		links.addLink(link2);
		links.addLink(link3);

		// Act:
		links.removeLink(link3);
		links.removeLink(link2);

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	// endregion

	// region getRemoteStatus

	@Test
	public void getRemoteStatusReturnsNotSetIfRemoteLinksIsEmpty() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Assert:
		MatcherAssert.assertThat(links.getRemoteStatus(BlockHeight.ONE), IsEqual.equalTo(RemoteStatus.NOT_SET));
		MatcherAssert.assertThat(links.getRemoteStatus(new BlockHeight(100L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
		MatcherAssert.assertThat(links.getRemoteStatus(new BlockHeight(1000L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
		MatcherAssert.assertThat(links.getRemoteStatus(new BlockHeight(10000L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
	}

	@Test
	public void getRemoteStatusReturnsOwnerActivatingIfCurrentModeIsActivateAndOwnerIsHarvestingRemotelyAndActivationWasWithinLimit() {
		// Assert:
		assertRemoteStatusWithinLimit(ImportanceTransferMode.Activate, RemoteLink.Owner.HarvestingRemotely, RemoteStatus.OWNER_ACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsOwnerActiveIfCurrentModeIsActivateAndOwnerIsHarvestingRemotelyAndActivationAboveLimit() {
		// Assert:
		assertRemoteStatusAboveLimit(ImportanceTransferMode.Activate, RemoteLink.Owner.HarvestingRemotely, RemoteStatus.OWNER_ACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsOwnerDeactivatingIfCurrentModeIsDeactivateAndOwnerIsHarvestingRemotelyAndDeactivationWasWithinLimit() {
		// Assert:
		assertRemoteStatusWithinLimit(ImportanceTransferMode.Deactivate, RemoteLink.Owner.HarvestingRemotely,
				RemoteStatus.OWNER_DEACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsOwnerInactiveIfCurrentModeIsDeactivateAndOwnerIsHarvestingRemotelyAndDeactivationIsAboveLimit() {
		// Assert:
		assertRemoteStatusAboveLimit(ImportanceTransferMode.Deactivate, RemoteLink.Owner.HarvestingRemotely, RemoteStatus.OWNER_INACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsRemoteActivatingIfCurrentModeIsActivateAndOwnerIsRemoteHarvesterAndActivationWasWithinLimit() {
		// Assert:
		assertRemoteStatusWithinLimit(ImportanceTransferMode.Activate, RemoteLink.Owner.RemoteHarvester, RemoteStatus.REMOTE_ACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsRemoteActiveIfCurrentModeIsActivateAndOwnerIsRemoteHarvesterAndActivationIsAboveLimit() {
		// Assert:
		assertRemoteStatusAboveLimit(ImportanceTransferMode.Activate, RemoteLink.Owner.RemoteHarvester, RemoteStatus.REMOTE_ACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsRemoteDeactivatingIfCurrentModeIsDeactivateAndOwnerIsRemoteHarvesterAndDeactivationIsWithinLimit() {
		// Assert:
		assertRemoteStatusWithinLimit(ImportanceTransferMode.Deactivate, RemoteLink.Owner.RemoteHarvester,
				RemoteStatus.REMOTE_DEACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsRemoteInactiveIfCurrentModeIsDeactivateAndOwnerIsRemoteHarvesterAndDeactivationIsAboveLimit() {
		// Assert:
		assertRemoteStatusAboveLimit(ImportanceTransferMode.Deactivate, RemoteLink.Owner.RemoteHarvester, RemoteStatus.REMOTE_INACTIVE);
	}

	private static void assertRemoteStatusWithinLimit(final ImportanceTransferMode mode, final RemoteLink.Owner owner,
			final RemoteStatus expectedStatus) {
		final int linkHeight = 123;
		assertRemoteStatus(new BlockHeight(linkHeight), new BlockHeight(linkHeight), mode, owner, expectedStatus);
		assertRemoteStatus(new BlockHeight(linkHeight + NisTestConstants.REMOTE_HARVESTING_DELAY - 1), new BlockHeight(linkHeight), mode,
				owner, expectedStatus);
	}

	private static void assertRemoteStatusAboveLimit(final ImportanceTransferMode mode, final RemoteLink.Owner owner,
			final RemoteStatus expectedStatus) {
		final int linkHeight = 123;
		assertRemoteStatus(new BlockHeight(linkHeight + NisTestConstants.REMOTE_HARVESTING_DELAY), new BlockHeight(linkHeight), mode, owner,
				expectedStatus);
		assertRemoteStatus(new BlockHeight(linkHeight + NisTestConstants.REMOTE_HARVESTING_DELAY + 1), new BlockHeight(linkHeight), mode,
				owner, expectedStatus);
	}

	private static void assertRemoteStatus(final BlockHeight height, final BlockHeight linkHeight, final ImportanceTransferMode mode,
			final RemoteLink.Owner owner, final RemoteStatus expectedStatus) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final RemoteLinks links = new RemoteLinks();
		links.addLink(new RemoteLink(address, linkHeight, mode, owner));

		// Act:
		final RemoteStatus status = links.getRemoteStatus(height);

		// Assert:
		MatcherAssert.assertThat(status, IsEqual.equalTo(expectedStatus));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllLinks() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		final RemoteLink link1 = createRandomLink();
		final RemoteLink link2 = createRandomLink();
		final RemoteLink link3 = createRandomLink();
		links.addLink(link1);
		links.addLink(link2);
		links.addLink(link3);

		// Act:
		final RemoteLinks copy = links.copy();

		// Assert:
		copy.removeLink(link3);
		copy.removeLink(link2);

		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(copy.isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(copy.getCurrent(), IsNull.nullValue());
	}

	// endregion

	private static RemoteLink createRandomLink() {
		return RemoteLinkFactory.activateRemoteHarvester(Utils.generateRandomAddress(), new BlockHeight(7));
	}
}
