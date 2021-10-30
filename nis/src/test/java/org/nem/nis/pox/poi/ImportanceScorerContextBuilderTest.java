package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

public class ImportanceScorerContextBuilderTest {

	@Test
	public void canCreateContext() {
		// Act:
		final ImportanceScorerContextBuilder builder = new ImportanceScorerContextBuilder();
		builder.setImportanceVector(new ColumnVector(1, 1, 1));
		builder.setOutlinkVector(new ColumnVector(2, 2, 2));
		builder.setVestedBalanceVector(new ColumnVector(3, 3, 3));
		builder.setGraphWeightVector(new ColumnVector(4, 4, 4));
		final ImportanceScorerContext context = builder.create();

		// Assert:
		MatcherAssert.assertThat(context.getImportanceVector(), IsEqual.equalTo(new ColumnVector(1, 1, 1)));
		MatcherAssert.assertThat(context.getOutlinkVector(), IsEqual.equalTo(new ColumnVector(2, 2, 2)));
		MatcherAssert.assertThat(context.getVestedBalanceVector(), IsEqual.equalTo(new ColumnVector(3, 3, 3)));
		MatcherAssert.assertThat(context.getGraphWeightVector(), IsEqual.equalTo(new ColumnVector(4, 4, 4)));
	}
}
