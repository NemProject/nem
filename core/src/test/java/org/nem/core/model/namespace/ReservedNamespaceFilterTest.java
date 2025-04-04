package org.nem.core.model.namespace;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

public class ReservedNamespaceFilterTest {
	private static final String[] EXPECTED_RESERVED_ROOTS = {
			"nem", "user", "account", "org", "com", "biz", "net", "edu", "mil", "gov", "info"
	};

	@Test
	public void setContainsAllExpectedValues() {
		// Assert:
		MatcherAssert.assertThat(ReservedNamespaceFilter.getAll().size(), IsEqual.equalTo(EXPECTED_RESERVED_ROOTS.length));
		MatcherAssert.assertThat(ReservedNamespaceFilter.getAll().stream().map(NamespaceIdPart::toString).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(EXPECTED_RESERVED_ROOTS));
	}

	@Test
	public void isClaimableReturnsFalseForRootNamespaceIdPartsInTheSet() {
		// Assert:
		Arrays.stream(EXPECTED_RESERVED_ROOTS).map(NamespaceId::new)
				.forEach(nid -> MatcherAssert.assertThat(ReservedNamespaceFilter.isClaimable(nid), IsEqual.equalTo(false)));
	}

	@Test
	public void isClaimableReturnsTrueForRootNamespaceIdPartsNotInTheSet() {
		// Assert:
		Arrays.stream(new String[]{
				"xyz", "foo", "bar"
		}).map(NamespaceId::new).forEach(nid -> MatcherAssert.assertThat(ReservedNamespaceFilter.isClaimable(nid), IsEqual.equalTo(true)));
	}

	@Test
	public void rootReservedNamespaceIdPartIsProhibited() {
		// Act:
		final boolean isClaimable = ReservedNamespaceFilter.isClaimable(new NamespaceId("com.google.images"));

		// Assert:
		MatcherAssert.assertThat(isClaimable, IsEqual.equalTo(false));
	}

	@Test
	public void middleReservedNamespaceIdPartIsProhibited() {
		// Act:
		final boolean isClaimable = ReservedNamespaceFilter.isClaimable(new NamespaceId("google.com.google"));

		// Assert:
		MatcherAssert.assertThat(isClaimable, IsEqual.equalTo(false));
	}

	@Test
	public void leafReservedNamespaceIdPartIsProhibited() {
		// Act:
		final boolean isClaimable = ReservedNamespaceFilter.isClaimable(new NamespaceId("google.images.com"));

		// Assert:
		MatcherAssert.assertThat(isClaimable, IsEqual.equalTo(false));
	}
}
