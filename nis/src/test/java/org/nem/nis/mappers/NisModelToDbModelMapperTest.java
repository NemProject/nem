package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.nis.dbmodel.DbBlock;

public class NisModelToDbModelMapperTest {

	@Test
	public void mapBlockDelegatesToInnerMapper() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);
		final NisModelToDbModelMapper nisMapper = new NisModelToDbModelMapper(mapper);

		final Block block = Mockito.mock(Block.class);
		final DbBlock dbBlock = Mockito.mock(DbBlock.class);
		Mockito.when(mapper.map(block, DbBlock.class)).thenReturn(dbBlock);

		// Act:
		final DbBlock result = nisMapper.map(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(dbBlock));
		Mockito.verify(mapper, Mockito.only()).map(block, DbBlock.class);
	}
}
