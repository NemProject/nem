package org.nem.nis.visitors;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.poi.PoiImportanceGenerator;

public class UndoBlockVisitorTest {

	@Test
	public void visitorCallsUndoOnBlock() {
		// Arrange:
		final Block block = Mockito.mock(Block.class);
		final UndoBlockVisitor visitor = new UndoBlockVisitor(createAccountAnalyzer());

		// Act:
		visitor.visit(null, block);

		// Assert:
		Mockito.verify(block, Mockito.times(1)).undo();
	}

	private static AccountAnalyzer createAccountAnalyzer() {
		return new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
	}
}
