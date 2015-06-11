package org.nem.core.model.namespace;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.Arrays;

public class ReservedRootNamespacesTest {
	final static String[] expectedReservedRoots = { "nem", "user", "account", "org", "com", "biz", "net", "edu", "mil", "gov", "info" };

	@Test
	public void setSizesMatchExpectedValue() {
		// Assert:
		Assert.assertThat(ReservedRootNamespaces.getAll().size(), IsEqual.equalTo(expectedReservedRoots.length));
	}

	@Test
	public void setContainsAllExpectedValues() {
		// Assert:
		Arrays.stream(expectedReservedRoots).forEach(r -> Assert.assertThat(ReservedRootNamespaces.contains(new NamespaceId(r)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseForNamespaceIdsNotInTheSet() {
		// Assert:
		Assert.assertThat(ReservedRootNamespaces.contains(new NamespaceId("xyz")), IsEqual.equalTo(false));
		Assert.assertThat(ReservedRootNamespaces.contains(new NamespaceId("foo")), IsEqual.equalTo(false));
		Assert.assertThat(ReservedRootNamespaces.contains(new NamespaceId("bar")), IsEqual.equalTo(false));
	}
}
