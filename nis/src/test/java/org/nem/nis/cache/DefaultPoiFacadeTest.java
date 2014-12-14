package org.nem.nis.cache;

import org.nem.nis.poi.ImportanceCalculator;

public class DefaultPoiFacadeTest extends PoiFacadeTest<DefaultPoiFacade> {

	@Override
	protected DefaultPoiFacade createPoiFacade(final ImportanceCalculator importanceCalculator) {
		return new DefaultPoiFacade(importanceCalculator);
	}
}
