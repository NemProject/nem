package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;

public class MatrixElementTest {

	@Test
	public void canCreateMatrixElement() {
		// Act + Assert:
		new MatrixElement(5,3,2.34);
	}

	//region equals / hashCode

	@Test
	public void equalsReturnsFalseForNonMatrixEntryObjects() {
		// Arrange:
		final MatrixElement matrixElement = new MatrixElement(0, 1, 2.0);

		// Assert:
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(matrixElement)));
		Assert.assertThat(new double[] { 0, 0, 7, 0, 0, 5 }, IsNot.not(IsEqual.equalTo((Object)matrixElement)));
	}

	@Test
	public void equalsReturnsFalseForDifferentMatrixEntries() {
		// Arrange:
		final MatrixElement matrixElement = new MatrixElement(0, 1, 2.0);

		// Assert:
		Assert.assertThat(matrixElement, IsNot.not(IsEqual.equalTo(new MatrixElement(1, 1, 2.0))));
		Assert.assertThat(matrixElement, IsNot.not(IsEqual.equalTo(new MatrixElement(0, 0, 2.0))));
		Assert.assertThat(matrixElement, IsNot.not(IsEqual.equalTo(new MatrixElement(0, 1, 3.0))));
	}

	@Test
	public void equalsReturnsTrueForEqualMatrixElements() {
		// Arrange:
		final MatrixElement matrixElement = new MatrixElement(0, 1, 2.0);

		// Assert:
		Assert.assertThat(matrixElement, IsEqual.equalTo(new MatrixElement(0, 1, 2.0)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentMatrixEntries() {
		// Arrange:
		final MatrixElement matrixElement = new MatrixElement(0, 1, 2.0);

		// Assert:
		Assert.assertThat(matrixElement.hashCode(), IsEqual.equalTo(new MatrixElement(0, 1, 2.0).hashCode()));
		Assert.assertThat(matrixElement.hashCode(), IsEqual.equalTo(new MatrixElement(0, 1, 3.0).hashCode()));
	}

	@Test
	public void hashCodesAreNotEqualForInequivalentMatrixEntries() {
		// Arrange:
		final MatrixElement matrixElement = new MatrixElement(0, 1, 2.0);

		// Assert:
		Assert.assertThat(matrixElement.hashCode(), IsNot.not(IsEqual.equalTo(new MatrixElement(1, 1, 2.0).hashCode())));
		Assert.assertThat(matrixElement.hashCode(), IsNot.not(IsEqual.equalTo(new MatrixElement(0, 0, 2.0).hashCode())));
	}

	//endregion
}
