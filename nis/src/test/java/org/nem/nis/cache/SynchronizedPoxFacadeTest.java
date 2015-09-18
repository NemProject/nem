package org.nem.nis.cache;

import org.nem.nis.poi.ImportanceCalculator;

public class SynchronizedPoxFacadeTest extends PoxFacadeTest<SynchronizedPoxFacade> {

	@Override
	protected SynchronizedPoxFacade createPoxFacade(final ImportanceCalculator importanceCalculator) {
		return new SynchronizedPoxFacade(new DefaultPoxFacade(importanceCalculator));
	}
}
