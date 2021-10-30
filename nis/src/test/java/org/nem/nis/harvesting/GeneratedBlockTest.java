package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Block;
import org.nem.nis.test.NisUtils;

public class GeneratedBlockTest {

	@Test
	public void canCreateGeneratedBlock() {
		// Arrange:
		final Block block = NisUtils.createRandomBlock();
		final long score = 18245L;

		// Act:
		final GeneratedBlock generatedBlock = new GeneratedBlock(block, score);

		// Assert:
		MatcherAssert.assertThat(generatedBlock.getBlock(), IsEqual.equalTo(block));
		MatcherAssert.assertThat(generatedBlock.getScore(), IsEqual.equalTo(score));
	}
}
