package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.Utils;

public class MosaicDefinitionMetaDataPairTest extends AbstractMetaDataPairTest<MosaicDefinition, MosaicMetaData> {

	public MosaicDefinitionMetaDataPairTest() {
		super(
				Utils::createMosaicDefinition,
				id -> new MosaicMetaData((long)id, Supply.ZERO),
				MosaicDefinitionMetaDataPair::new,
				MosaicDefinitionMetaDataPair::new,
				mosaicDefinition -> mosaicDefinition.getCreator().getAddress(),
				metaData -> metaData.getId().intValue());
	}
}
