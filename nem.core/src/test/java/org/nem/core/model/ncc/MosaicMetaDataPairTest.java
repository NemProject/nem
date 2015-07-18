package org.nem.core.model.ncc;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.*;

public class MosaicMetaDataPairTest extends AbstractMetaDataPairTest<Mosaic, DefaultMetaData> {

	public MosaicMetaDataPairTest() {
		super(
				address -> Utils.createMosaic(new Account(address)),
				id -> new DefaultMetaData((long)id),
				MosaicMetaDataPair::new,
				MosaicMetaDataPair::new,
				mosaic -> mosaic.getCreator().getAddress(),
				metaData -> metaData.getId().intValue());
	}
}
