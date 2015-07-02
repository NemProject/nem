package org.nem.nis.cache;

import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.BlockHeight;

public class DefaultMosaicCache implements MosaicCache, CopyableCache<DefaultMosaicCache> {

	@Override
	public void shallowCopyTo(final DefaultMosaicCache rhs) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public DefaultMosaicCache copy() {
		return new DefaultMosaicCache();
	}
}
