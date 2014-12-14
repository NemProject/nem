package org.nem.nis.cache;

import org.nem.nis.poi.ImportanceCalculator;

public class SynchronizedPoiFacadeTest extends PoiFacadeTest<SynchronizedPoiFacade> {

	@Override
	protected SynchronizedPoiFacade createPoiFacade(final ImportanceCalculator importanceCalculator) {
		return new SynchronizedPoiFacade(new DefaultPoiFacade(importanceCalculator));
	}
}
