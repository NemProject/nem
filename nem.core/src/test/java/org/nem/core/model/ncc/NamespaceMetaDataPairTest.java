package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class NamespaceMetaDataPairTest {

	@Test
	public void canCreateNamespaceMetaDataPair() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), new BlockHeight(17));
		final NamespaceMetaData metaData = new NamespaceMetaData(123L);

		// Act:
		final NamespaceMetaDataPair entity = new NamespaceMetaDataPair(namespace, metaData);

		// Assert:
		Assert.assertThat(entity.getNamespace(), IsSame.sameInstance(namespace));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripNamespaceMetaDataPair() {
		// Arrange:
		final Account owner = Utils.generateRandomAccount();

		// Act:
		final NamespaceMetaDataPair metaDataPair = createRoundTrippedPair(owner, 5678);

		// Assert:
		Assert.assertThat(metaDataPair.getNamespace().getOwner(), IsEqual.equalTo(owner));
		Assert.assertThat(metaDataPair.getMetaData().getId(), IsEqual.equalTo(5678L));
	}

	private static NamespaceMetaDataPair createRoundTrippedPair(final Account owner, final long id) {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), owner, new BlockHeight(17));
		final NamespaceMetaData metaData = new NamespaceMetaData(id);
		final NamespaceMetaDataPair entity = new NamespaceMetaDataPair(namespace, metaData);

		// Act:
		return new NamespaceMetaDataPair(Utils.roundtripSerializableEntity(entity, new MockAccountLookup()));
	}
}