package org.nem.nis.cache;

import org.nem.nis.pox.ImportanceCalculator;

public class DefaultPoxFacadeTest extends PoxFacadeTest<DefaultPoxFacade> {

	@Override
	protected DefaultPoxFacade createPoxFacade(final ImportanceCalculator importanceCalculator) {
		return new DefaultPoxFacade(importanceCalculator);
	}
}
