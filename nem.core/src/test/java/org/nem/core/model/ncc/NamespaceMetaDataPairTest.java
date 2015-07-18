package org.nem.core.model.ncc;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

public class NamespaceMetaDataPairTest extends AbstractMetaDataPairTest<Namespace, DefaultMetaData> {

	public NamespaceMetaDataPairTest() {
		super(
				address -> new Namespace(new NamespaceId("foo"), new Account(address), new BlockHeight(17)),
				id -> new DefaultMetaData((long)id),
				NamespaceMetaDataPair::new,
				NamespaceMetaDataPair::new,
				namespace -> namespace.getOwner().getAddress(),
				metaData -> metaData.getId().intValue());
	}
}