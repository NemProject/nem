package org.nem.peer.trust;

/**
 * Represents a linear algebra matrix.
 */
public class Matrix {

    final int rows;
    final int cols;
    final Vector[] columns;

    /**
     * Creates a new matrix of the specified size.
     *
     * @param rows The desired number of rows.
     * @param cols The desired number of columns.
     */
    public Matrix(final int rows, final int cols) {
        this.rows = rows;
        this.cols = cols;
        this.columns = new Vector[this.cols];
        for (int i = 0; i < this.cols; ++i)
            this.columns[i] = new Vector(this.rows);
    }

    /**
     * Gets the number of rows.
     *
     * @return The number of rows.
     */
    public int getRowCount() { return this.rows; }

    /**
     * Gets the number of columns.
     *
     * @return The number of columns.
     */
    public int getColumnCount() { return this.cols; }

    /**
     * Gets the value at the specified row and column.
     *
     * @param row The row.
     * @param col The column.
     * @return The value.
     */
    public double getAt(final int row, final int col) {
        return this.columns[col].getAt(row);
    }

    /**
     * Sets a value at the specified row and column.
     *
     * @param row The row.
     * @param col The column.
     * @param val The value.
     */
    public void setAt(final int row, final int col, final double val) {
        this.columns[col].setAt(row, val);
    }

    /**
     * Transposes this matrix.
     *
     * @return A transposed matrix.
     */
    public Matrix transpose() {
        final Matrix transposedMatrix = new Matrix(this.cols, this.rows);
        for (int i = 0; i < this.rows; ++i) {
            for (int j = 0; j < this.cols; ++j ) {
                transposedMatrix.columns[i].setAt(j, this.columns[j].getAt(i));
            }
        }

        return transposedMatrix;
    }

    /**
     * Normalizes each column of the matrix.
     */
    public void normalizeColumns() {
        for (int i = 0; i < this.cols; ++i)
            this.columns[i].normalize();
    }
}