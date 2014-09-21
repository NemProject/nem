package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

public class RemoteLinksTest {

	//region constructor

	@Test
	public void remotesAreNotSetUpByDefault() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		// Assert:
		Assert.assertThat(states.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(states.getCurrent(), IsNull.nullValue());
	}

	//endregion

	//region addLink

	@Test
	public void canAddLinkMarkingAccountAsHarvestingRemotely() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		// Act:
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), 1, RemoteLink.Owner.HarvestingRemotely);
		states.addLink(link);

		// Assert:
		Assert.assertThat(states.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(states.getCurrent(), IsEqual.equalTo(link));
	}

	@Test
	public void canAddLinkMarkingAccountAsRemoteHarvester() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		// Act:
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);
		states.addLink(link);

		// Assert:
		Assert.assertThat(states.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(states.getCurrent(), IsEqual.equalTo(link));
	}

	//endregion

	//region removeLink

	@Test
	public void cannotRemoveLinkWhenNoLinksArePresent() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		// Act:
		ExceptionAssert.assertThrows(v -> states.removeLink(createRandomLink()), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemoveInconsistentLink() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();
		states.addLink(createRandomLink());

		// Act:
		ExceptionAssert.assertThrows(v -> states.removeLink(createRandomLink()), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveConsistentLink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final RemoteLinks states = new RemoteLinks();

		final RemoteLink link1 = new RemoteLink(address, new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);
		states.addLink(link1);

		// Act:
		final RemoteLink link2 = new RemoteLink(address, new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);
		states.removeLink(link2);

		// Assert:
		Assert.assertThat(states.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(states.getCurrent(), IsNull.nullValue());
	}

	//endregion

	//region storage

	@Test
	public void canStoreMaximumOfTwoLinks() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		final RemoteLink link1 = createRandomLink();
		final RemoteLink link2 = createRandomLink();
		final RemoteLink link3 = createRandomLink();
		states.addLink(link1);
		states.addLink(link2);
		states.addLink(link3);

		// Act:
		states.removeLink(link3);
		states.removeLink(link2);

		// Assert:
		Assert.assertThat(states.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(states.getCurrent(), IsNull.nullValue());
	}

	//endregion

	//region copy

	@Test
	public void copyCopiesAllLinks() {
		// Arrange:
		final RemoteLinks states = new RemoteLinks();

		final RemoteLink link1 = createRandomLink();
		final RemoteLink link2 = createRandomLink();
		final RemoteLink link3 = createRandomLink();
		states.addLink(link1);
		states.addLink(link2);
		states.addLink(link3);

		// Act:
		final RemoteLinks copy = states.copy();

		// Assert:
		copy.removeLink(link3);
		copy.removeLink(link2);

		Assert.assertThat(copy.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(copy.getCurrent(), IsNull.nullValue());
	}

	//endregion

	private static RemoteLink createRandomLink() {
		return new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);
	}
}