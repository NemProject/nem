package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;

public class NisModelToDbModelMapperTest {

	@Test
	public void mapBlockDelegatesToInnerMapper() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);
		final NisModelToDbModelMapper nisMapper = new NisModelToDbModelMapper(mapper);

		final Block block = Mockito.mock(Block.class);
		final org.nem.nis.dbmodel.Block dbBlock = Mockito.mock(org.nem.nis.dbmodel.Block.class);
		Mockito.when(mapper.map(block, org.nem.nis.dbmodel.Block.class)).thenReturn(dbBlock);

		// Act:
		final org.nem.nis.dbmodel.Block result = nisMapper.map(block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(dbBlock));
		Mockito.verify(mapper, Mockito.only()).map(block, org.nem.nis.dbmodel.Block.class);
	}
}