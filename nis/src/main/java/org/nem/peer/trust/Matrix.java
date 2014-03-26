package org.nem.peer.trust;

import java.text.DecimalFormat;

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

    /**
     * Gets the sum of the absolute value of all the matrix's elements.
     *
     * @return The sum of the absolute value of all the matrix's elements.
     */
    public double absSum() {
        double sum = 0.0;
        for (int i = 0; i < this.cols; ++i)
            sum += this.columns[i].absSum();

        return sum;
    }

    @Override
    public String toString() {
        final DecimalFormat format = new DecimalFormat("#0.000");
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.rows; ++i) {
            if (0 != i)
                builder.append(System.lineSeparator());

            for (int j = 0; j < this.cols; ++j ) {
                if (0 != j)
                    builder.append(" ");

                builder.append(format.format(this.getAt(i, j)));
            }
        }

        return builder.toString();
    }
}