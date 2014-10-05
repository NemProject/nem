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
	 * @param file The file.
	 */
	public static void save(final Matrix matrix, final File file) {
		ExceptionUtils.propagateVoid(
				() -> {
					saveJava(matrix, file);
					saveJson(matrix, new File(file  + ".json"));
				});
	}

	private static void saveJava(final Matrix matrix, final File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file);
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

	private static void saveJson(final Matrix matrix, final File file) throws IOException {
		final JsonSerializer serializer = new JsonSerializer();
		for (int i = 0; i < matrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				serializer.writeDouble(element.getRow() + "," + element.getColumn(), element.getValue());
			}
		}

		final JSONObject object = serializer.getObject();
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(object.toJSONString());
		}
	}

	/**
	 * Loads a matrix from the specified file.
	 *
	 * @param file The file.
	 * @return matrix The matrix.
	 */
	public static Matrix load(final File file) {
		return ExceptionUtils.propagate(() -> loadJava(file));
	}

	private static Matrix loadJava(final File file) throws IOException, ClassNotFoundException {
		try (FileInputStream fis = new FileInputStream(file);
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
