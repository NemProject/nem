package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;

public class MatrixTest {

    //region constructor / getAt / setAt

    @Test
    public void matrixIsInitializedToZero() {
        // Arrange:
        final Matrix matrix = new Matrix(2, 3);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(0.0));
    }

    @Test
    public void matrixValuesCanBeSet() {
        // Arrange:
        final Matrix matrix = new Matrix(3, 2);

        // Act:
        matrix.setAt(0, 0, 7);
        matrix.setAt(0, 1, 3);
        matrix.setAt(1, 0, 5);
        matrix.setAt(1, 1, 11);
        matrix.setAt(2, 0, 1);
        matrix.setAt(2, 1, 9);

        // Assert:
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(7.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(3.0));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(5.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(11.0));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(9.0));
    }

    @Test
    public void matrixCannotBeIndexedOutOfBounds() {
        // Assert:
        assertOutOfBounds(2, 3, -1, 0);
        assertOutOfBounds(2, 3, 0, -1);
        assertOutOfBounds(2, 3, 2, 0);
        assertOutOfBounds(2, 3, 0, 3);
    }

    private static void assertOutOfBounds(final int numRows, int numCols, final int row, final int col) {
        try {
            // Arrange:
            final Matrix matrix = new Matrix(numRows, numCols);

            // Act:
            matrix.getAt(row, col);

            // Assert:
            Assert.fail("expected exception was not thrown");
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
    }

    //endregion

    //region transpose

    @Test
    public void matrixCanBeTransposed() {
        // Arrange:
        final Matrix matrix = new Matrix(3, 2);
        matrix.setAt(0, 0, 7);
        matrix.setAt(0, 1, 3);
        matrix.setAt(1, 0, 5);
        matrix.setAt(1, 1, 11);
        matrix.setAt(2, 0, 1);
        matrix.setAt(2, 1, 9);

        // Act:
        final Matrix transposedMatrix = matrix.transpose();

        // Assert:
        Assert.assertThat(transposedMatrix.getAt(0, 0), IsEqual.equalTo(7.0));
        Assert.assertThat(transposedMatrix.getAt(0, 1), IsEqual.equalTo(5.0));
        Assert.assertThat(transposedMatrix.getAt(0, 2), IsEqual.equalTo(1.0));
        Assert.assertThat(transposedMatrix.getAt(1, 0), IsEqual.equalTo(3.0));
        Assert.assertThat(transposedMatrix.getAt(1, 1), IsEqual.equalTo(11.0));
        Assert.assertThat(transposedMatrix.getAt(1, 2), IsEqual.equalTo(9.0));
    }

    //endregion

    // normalizeColumns

    @Test
    public void allMatrixColumnsCanBeNormalized() {
        // Arrange:
        final Matrix matrix = new Matrix(3, 2);
        matrix.setAt(0, 0, 2);
        matrix.setAt(1, 0, 3);
        matrix.setAt(2, 0, 5);
        matrix.setAt(0, 1, 11);
        matrix.setAt(1, 1, 1);
        matrix.setAt(2, 1, 8);

        // Act:
        matrix.normalizeColumns();

        // Assert:
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.2));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.3));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(0.5));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.55));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.05));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(0.4));
    }

    //endregion
}
