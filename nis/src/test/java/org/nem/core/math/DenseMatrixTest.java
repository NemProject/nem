package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;

public class DenseMatrixTest extends MatrixTest {

	//region toString

	@Test
	public void denseMatrixStringRepresentationIsCorrect() {
		// Arrange:
		final Matrix matrix = this.createMatrix(3, 2, new double[] {
				2.1234, 11.1234, 3.2345, 1, 5012.0126, 8
		});

		// Assert:
		final String expectedResult =
				"2.123 11.123" + System.lineSeparator() +
						"3.235 1.000" + System.lineSeparator() +
						"5012.013 8.000";
		Assert.assertThat(matrix.toString(), IsEqual.equalTo(expectedResult));
	}

	//endregion

	@Override
	protected Matrix createMatrix(int rows, int cols) {
		return new DenseMatrix(rows, cols);
	}
}
