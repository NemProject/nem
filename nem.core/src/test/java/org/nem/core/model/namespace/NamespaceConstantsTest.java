package org.nem.core.model.namespace;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

public class NamespaceConstantsTest {
	private static final PublicKey LESSOR_PUBLIC_KEY = PublicKey.fromHexString("f907bac7f3f162efeb48912a8c4f5dfbd4f3d2305e8a033e75216dc6f16cc894");

	@Test
	public void lessorHasExpectedPublicKey() {
		// Assert:
		Assert.assertThat(NamespaceConstants.LESSOR.getAddress().getPublicKey(), IsEqual.equalTo(LESSOR_PUBLIC_KEY));
	}

	@Test
	public void rootNamespaceNemHasExpectedOwner() {
		// Assert:
		Assert.assertThat(NamespaceConstants.ROOT_NAMESPACE_NEM.getOwner().getAddress(), IsEqual.equalTo(Address.fromPublicKey(LESSOR_PUBLIC_KEY)));
	}

	@Test
	public void rootNamespaceNemHasExpectedBlockHeight() {
		// Assert:
		Assert.assertThat(NamespaceConstants.ROOT_NAMESPACE_NEM.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}
}
