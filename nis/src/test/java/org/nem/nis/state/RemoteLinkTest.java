package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

import java.util.*;

public class RemoteLinkTest {
	private static final ImportanceTransferMode ACTIVATE = ImportanceTransferMode.Activate;
	private static final ImportanceTransferMode DEACTIVATE = ImportanceTransferMode.Deactivate;

	@Test
	public void canCreateRemoteLink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final RemoteLink link = new RemoteLink(address, new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.RemoteHarvester);

		// Assert:
		MatcherAssert.assertThat(link.getLinkedAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(link.getEffectiveHeight(), IsEqual.equalTo(new BlockHeight(11)));
		MatcherAssert.assertThat(link.getMode(), IsEqual.equalTo(DEACTIVATE));
		MatcherAssert.assertThat(link.getOwner(), IsEqual.equalTo(RemoteLink.Owner.RemoteHarvester));
	}

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, RemoteLink> DESC_TO_LINK_MAP = new HashMap<String, RemoteLink>() {
		{
			this.put("default",
					new RemoteLink(Address.fromEncoded("a"), new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.RemoteHarvester));
			this.put("diff-address",
					new RemoteLink(Address.fromEncoded("b"), new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.RemoteHarvester));
			this.put("diff-height",
					new RemoteLink(Address.fromEncoded("a"), new BlockHeight(10), DEACTIVATE, RemoteLink.Owner.RemoteHarvester));
			this.put("diff-mode",
					new RemoteLink(Address.fromEncoded("a"), new BlockHeight(11), ACTIVATE, RemoteLink.Owner.RemoteHarvester));
			this.put("diff-owner",
					new RemoteLink(Address.fromEncoded("a"), new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.HarvestingRemotely));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final RemoteLink link = new RemoteLink(Address.fromEncoded("a"), new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.RemoteHarvester);

		// Assert:
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("default"), IsEqual.equalTo(link));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-address"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-height"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-mode"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-owner"), IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(link)));
		MatcherAssert.assertThat(Address.fromEncoded("a"), IsNot.not(IsEqual.equalTo((Object) link)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final RemoteLink link = new RemoteLink(Address.fromEncoded("a"), new BlockHeight(11), DEACTIVATE, RemoteLink.Owner.RemoteHarvester);
		final int hashCode = link.hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-address").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-height").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-mode").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_LINK_MAP.get("diff-owner").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion
}
