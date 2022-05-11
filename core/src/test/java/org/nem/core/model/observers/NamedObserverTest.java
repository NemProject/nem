package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NamedObserverTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final NamedObserver observer = new CrazyNameObserver();

		// Act:
		final String name = observer.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("CrazyNameObserver"));
	}

	private static class CrazyNameObserver implements NamedObserver {
	}
}
