package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.test.Utils;

public class MosaicDefinitionMetaDataPairTest extends AbstractMetaDataPairTest<MosaicDefinition, DefaultMetaData> {

	public MosaicDefinitionMetaDataPairTest() {
		super(
				Utils::createMosaicDefinition,
				id -> new DefaultMetaData((long)id),
				MosaicDefinitionMetaDataPair::new,
				MosaicDefinitionMetaDataPair::new,
				mosaicDefinition -> mosaicDefinition.getCreator().getAddress(),
				metaData -> metaData.getId().intValue());
	}
}
