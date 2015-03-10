package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.BlockChainConstants;

public class RemoteLinksTest {
	private static final ImportanceTransferMode ACTIVATE = ImportanceTransferMode.Activate;

	//region constructor

	@Test
	public void remotesAreNotSetUpByDefault() {
		// Arrange:
		final ReadOnlyRemoteLinks links = new RemoteLinks();

		// Assert:
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(false));
		Assert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(false));
		Assert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	//endregion

	//region addLink

	@Test
	public void canAddLinkMarkingAccountAsHarvestingRemotely() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Act:
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), ACTIVATE, RemoteLink.Owner.HarvestingRemotely);
		links.addLink(link);

		// Assert:
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(true));
		Assert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(false));
		Assert.assertThat(links.getCurrent(), IsEqual.equalTo(link));
	}

	@Test
	public void canAddLinkMarkingAccountAsRemoteHarvester() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Act:
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), ACTIVATE, RemoteLink.Owner.RemoteHarvester);
		links.addLink(link);

		// Assert:
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(links.isHarvestingRemotely(), IsEqual.equalTo(false));
		Assert.assertThat(links.isRemoteHarvester(), IsEqual.equalTo(true));
		Assert.assertThat(links.getCurrent(), IsEqual.equalTo(link));
	}

	//endregion

	//region removeLink

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

		final RemoteLink link1 = new RemoteLink(address, new BlockHeight(7), ACTIVATE, RemoteLink.Owner.RemoteHarvester);
		links.addLink(link1);

		// Act:
		final RemoteLink link2 = new RemoteLink(address, new BlockHeight(7), ACTIVATE, RemoteLink.Owner.RemoteHarvester);
		links.removeLink(link2);

		// Assert:
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	//endregion

	//region storage

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
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(links.getCurrent(), IsNull.nullValue());
	}

	//endregion

	// region getRemoteStatus

	@Test
	public void getRemoteStatusReturnsNotSetIfRemoteLinksIsEmpty() {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();

		// Assert:
		Assert.assertThat(links.getRemoteStatus(BlockHeight.ONE), IsEqual.equalTo(RemoteStatus.NOT_SET));
		Assert.assertThat(links.getRemoteStatus(new BlockHeight(100L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
		Assert.assertThat(links.getRemoteStatus(new BlockHeight(1000L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
		Assert.assertThat(links.getRemoteStatus(new BlockHeight(10000L)), IsEqual.equalTo(RemoteStatus.NOT_SET));
	}

	@Test
	public void getRemoteStatusReturnsOwnerActivatingIfCurrentModeIsActivateAndOwnerIsHarvestingRemotelyAndActivationWasWithinOneDay() {
		// Assert:
		assertRemoteStatusWithinOneDay(
				ImportanceTransferMode.Activate,
				RemoteLink.Owner.HarvestingRemotely,
				RemoteStatus.OWNER_ACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsOwnerActiveIfCurrentModeIsActivateAndOwnerIsHarvestingRemotelyAndActivationIsLeastOneDayOld() {
		// Assert:
		assertRemoteStatusOlderThanOneDay(
				ImportanceTransferMode.Activate,
				RemoteLink.Owner.HarvestingRemotely,
				RemoteStatus.OWNER_ACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsOwnerDeactivatingIfCurrentModeIsDeactivateAndOwnerIsHarvestingRemotelyAndDeactivationWasWithinOneDay() {
		// Assert:
		assertRemoteStatusWithinOneDay(
				ImportanceTransferMode.Deactivate,
				RemoteLink.Owner.HarvestingRemotely,
				RemoteStatus.OWNER_DEACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsOwnerInactiveIfCurrentModeIsDeactivateAndOwnerIsHarvestingRemotelyAndDeactivationIsLeastOneDayOld() {
		// Assert:
		assertRemoteStatusOlderThanOneDay(
				ImportanceTransferMode.Deactivate,
				RemoteLink.Owner.HarvestingRemotely,
				RemoteStatus.OWNER_INACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsRemoteActivatingIfCurrentModeIsActivateAndOwnerIsRemoteHarvesterAndActivationWasWithinOneDay() {
		// Assert:
		assertRemoteStatusWithinOneDay(
				ImportanceTransferMode.Activate,
				RemoteLink.Owner.RemoteHarvester,
				RemoteStatus.REMOTE_ACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsRemoteActiveIfCurrentModeIsActivateAndOwnerIsRemoteHarvesterAndActivationIsLeastOneDayOld() {
		// Assert:
		assertRemoteStatusOlderThanOneDay(
				ImportanceTransferMode.Activate,
				RemoteLink.Owner.RemoteHarvester,
				RemoteStatus.REMOTE_ACTIVE);
	}

	@Test
	public void getRemoteStatusReturnsRemoteDeactivatingIfCurrentModeIsDeactivateAndOwnerIsRemoteHarvesterAndDeactivationWasWithinOneDay() {
		// Assert:
		assertRemoteStatusWithinOneDay(
				ImportanceTransferMode.Deactivate,
				RemoteLink.Owner.RemoteHarvester,
				RemoteStatus.REMOTE_DEACTIVATING);
	}

	@Test
	public void getRemoteStatusReturnsRemoteInactiveIfCurrentModeIsDeactivateAndOwnerIsRemoteHarvesterAndDeactivationIsLeastOneDayOld() {
		// Assert:
		assertRemoteStatusOlderThanOneDay(
				ImportanceTransferMode.Deactivate,
				RemoteLink.Owner.RemoteHarvester,
				RemoteStatus.REMOTE_INACTIVE);
	}

	private static void assertRemoteStatusWithinOneDay(
			final ImportanceTransferMode mode,
			final RemoteLink.Owner owner,
			final RemoteStatus expectedStatus) {
		final int linkHeight = 123;
		assertRemoteStatus(
				new BlockHeight(linkHeight),
				new BlockHeight(linkHeight),
				mode,
				owner,
				expectedStatus);
		assertRemoteStatus(
				new BlockHeight(linkHeight + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY - 1),
				new BlockHeight(linkHeight),
				mode,
				owner,
				expectedStatus);
	}

	private static void assertRemoteStatusOlderThanOneDay(
			final ImportanceTransferMode mode,
			final RemoteLink.Owner owner,
			final RemoteStatus expectedStatus) {
		final int linkHeight = 123;
		assertRemoteStatus(
				new BlockHeight(linkHeight + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				new BlockHeight(linkHeight),
				mode,
				owner,
				expectedStatus);
		assertRemoteStatus(
				new BlockHeight(linkHeight + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1),
				new BlockHeight(linkHeight),
				mode,
				owner,
				expectedStatus);
	}

	private static void assertRemoteStatus(
			final BlockHeight height,
			final BlockHeight linkHeight,
			final ImportanceTransferMode mode,
			final RemoteLink.Owner owner,
			final RemoteStatus expectedStatus) {
		// Arrange:
		final RemoteLinks links = new RemoteLinks();
		links.addLink(new RemoteLink(
				Utils.generateRandomAddress(),
				linkHeight,
				mode,
				owner));

		// Act:
		final RemoteStatus status = links.getRemoteStatus(height);

		// Assert:
		Assert.assertThat(status, IsEqual.equalTo(expectedStatus));
	}

	// endregion

	//region copy

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

		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(copy.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(copy.getCurrent(), IsNull.nullValue());
	}

	//endregion

	private static RemoteLink createRandomLink() {
		return new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), ACTIVATE, RemoteLink.Owner.RemoteHarvester);
	}
}