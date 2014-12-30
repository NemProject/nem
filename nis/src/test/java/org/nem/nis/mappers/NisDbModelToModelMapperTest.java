package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;

public class NisDbModelToModelMapperTest {

	@Test
	public void mapBlockDelegatesToInnerMapper() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);
		final NisDbModelToModelMapper nisMapper = new NisDbModelToModelMapper(mapper);

		final Block block = Mockito.mock(Block.class);
		final org.nem.nis.dbmodel.Block dbBlock = Mockito.mock(org.nem.nis.dbmodel.Block.class);
		Mockito.when(mapper.map(dbBlock, Block.class)).thenReturn(block);

		// Act:
		final Block result = nisMapper.map(dbBlock);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(block));
		Mockito.verify(mapper, Mockito.only()).map(dbBlock, Block.class);
	}
}