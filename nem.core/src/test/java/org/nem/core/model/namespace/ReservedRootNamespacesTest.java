package org.nem.core.model.namespace;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.Arrays;

public class ReservedRootNamespacesTest {
	final static String[] EXPECTED_RESERVED_ROOTS = { "nem", "user", "account", "org", "com", "biz", "net", "edu", "mil", "gov", "info" };

	@Test
	public void setSizesMatchExpectedValue() {
		// Assert:
		Assert.assertThat(ReservedRootNamespaces.getAll().size(), IsEqual.equalTo(EXPECTED_RESERVED_ROOTS.length));
	}

	@Test
	public void setContainsAllExpectedValues() {
		// Assert:
		Arrays.stream(EXPECTED_RESERVED_ROOTS)
				.map(s -> new NamespaceId(s))
				.forEach(nid -> Assert.assertThat(ReservedRootNamespaces.contains(nid), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseForNamespaceIdsNotInTheSet() {
		// Assert:
		Arrays.stream(new String[] { "xyz", "foo", "bar" })
				.map(s -> new NamespaceId(s))
				.forEach(nid -> Assert.assertThat(ReservedRootNamespaces.contains(nid), IsEqual.equalTo(false)));
	}
}
