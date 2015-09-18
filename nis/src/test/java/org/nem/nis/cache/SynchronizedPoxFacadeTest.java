package org.nem.nis.cache;

import org.nem.nis.pox.ImportanceCalculator;

public class SynchronizedPoxFacadeTest extends PoxFacadeTest<SynchronizedPoxFacade> {

	@Override
	protected SynchronizedPoxFacade createPoxFacade(final ImportanceCalculator importanceCalculator) {
		return new SynchronizedPoxFacade(new DefaultPoxFacade(importanceCalculator));
	}
}
