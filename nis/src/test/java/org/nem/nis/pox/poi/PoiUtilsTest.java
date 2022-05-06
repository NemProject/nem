package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

import java.util.Arrays;

public class PoiUtilsTest {

	@Test
	public void dangleSumIsCalculatedCorrectly() {
		// Act:
		final double dangleSum = PoiUtils.calculateDangleSum(Arrays.asList(1, 3), 0.4, new ColumnVector(0.1, 0.8, 0.2, 0.5, 0.6, 0.3));

		// Assert: sum(0.8, 0.5) * 0.4 / 6
		MatcherAssert.assertThat(dangleSum, IsEqual.equalTo(1.3 * 0.4 / 6));
	}
}
