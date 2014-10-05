package org.nem.nis.poi.graph;

import net.minidev.json.JSONObject;
import org.nem.core.math.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.utils.ExceptionUtils;

import java.io.*;

/**
 * A helper class for loading and saving matrices.
 */
public class MatrixRepository {

	/**
	 * Saves a matrix to the specified file.
	 *
	 * @param matrix The matrix.
	 * @param fileName The file.
	 * @throws IOException The matrix could not be saved.
	 */
	public static void save(final Matrix matrix, final String fileName) {
		ExceptionUtils.propagateVoid(
				() -> {
					saveJava(matrix, fileName);
					saveJson(matrix, fileName);
				});
	}

	private static void saveJava(final Matrix matrix, final String fileName) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(fileName);
			 ObjectOutputStream out = new ObjectOutputStream(fos)) {
			out.writeObject(matrix.getRowCount());
			out.writeObject(matrix.getColumnCount());

			for (int i = 0; i < matrix.getRowCount(); ++i) {
				final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(i);
				while (iterator.hasNext()) {
					final MatrixElement element = iterator.next();
					out.writeObject(element.getRow());
					out.writeObject(element.getColumn());
					out.writeObject(element.getValue());
				}
			}

			out.writeObject("EOF");
		}
	}

	private static void saveJson(final Matrix matrix, final String fileName) throws IOException {
		final JsonSerializer serializer = new JsonSerializer();
		for (int i = 0; i < matrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				serializer.writeDouble(element.getRow() + "," + element.getColumn(), element.getValue());
			}
		}

		final JSONObject object = serializer.getObject();
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(fileName + ".json"))) {
			writer.write(object.toJSONString());
		}
	}

	/**
	 * Loads a matrix from the specified file.
	 *
	 * @param fileName The file.
	 * @return matrix The matrix.
	 */
	public static Matrix load(final String fileName) {
		return ExceptionUtils.propagate(() -> loadJava(fileName));
	}

	private static Matrix loadJava(final String fileName) throws IOException, ClassNotFoundException {

		try (FileInputStream fis = new FileInputStream(fileName);
			 ObjectInputStream in = new ObjectInputStream(fis)) {
			final int rowCount = (int)in.readObject();
			final int columnCount = (int)in.readObject();
			final SparseMatrix matrix = new SparseMatrix(rowCount, columnCount, 8);

			while (true) {
				final Object object = in.readObject();
				if (object instanceof String) {
					break;
				}

				final int row = (int)object;
				final int col = (int)in.readObject();
				final double value = (double)in.readObject();
				matrix.setAt(row, col, value);
			}

			return matrix;
		}
	}
}
