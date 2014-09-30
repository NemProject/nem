package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;

import java.util.*;

public class MatrixElementTest {

	@Test
	public void canCreateMatrixElement() {
		// Act:
		final MatrixElement element = new MatrixElement(5, 3, 2.34);

		// Assert:
		Assert.assertThat(element.getRow(), IsEqual.equalTo(5));
		Assert.assertThat(element.getColumn(), IsEqual.equalTo(3));
		Assert.assertThat(element.getValue(), IsEqual.equalTo(2.34));
	}

	//region equals / hashCode

	private static final Map<String, MatrixElement> DESC_TO_ELEMENT_MAP = new HashMap<String, MatrixElement>() {
		{
			this.put("default", new MatrixElement(5, 4, 7.0));
			this.put("diff-row", new MatrixElement(6, 4, 7.0));
			this.put("diff-col", new MatrixElement(5, 3, 7.0));
			this.put("diff-val", new MatrixElement(5, 4, 7.2));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MatrixElement element = new MatrixElement(5, 4, 7.0);

		// Assert:
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("default"), IsEqual.equalTo(element));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-row"), IsNot.not(IsEqual.equalTo(element)));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-col"), IsNot.not(IsEqual.equalTo(element)));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-val"), IsNot.not(IsEqual.equalTo(element)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(element)));
		Assert.assertThat(5, IsNot.not(IsEqual.equalTo((Object)element)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final MatrixElement element = new MatrixElement(5, 4, 7.0);
		final int hashCode = element.hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-row").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-col").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_ELEMENT_MAP.get("diff-val").hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion
}
