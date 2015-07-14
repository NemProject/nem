package org.nem.core.model.namespace;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Constants used by namespace related classes.
 */
public class NamespaceConstants {
	private static final PublicKey LESSOR_PUBLIC_KEY = PublicKey.fromHexString("f907bac7f3f162efeb48912a8c4f5dfbd4f3d2305e8a033e75216dc6f16cc894");
	public static final Account LESSOR = new Account(Address.fromPublicKey(LESSOR_PUBLIC_KEY));
	public static final NamespaceId NAMESPACE_ID_NEM = new NamespaceId("nem");
	public static final Namespace ROOT_NAMESPACE_NEM = new Namespace(NAMESPACE_ID_NEM, LESSOR, BlockHeight.MAX);
}
