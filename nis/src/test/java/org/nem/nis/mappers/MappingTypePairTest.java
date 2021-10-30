package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;

import java.util.*;

@SuppressWarnings("rawtypes")
public class MappingTypePairTest {

	@Test
	public void pairCanBeCreated() {
		// Act:
		final MappingTypePair pair = new MappingTypePair(Integer.class, String.class);

		// Assert:
		MatcherAssert.assertThat(pair.getSourceClass(), IsEqual.equalTo(Integer.class));
		MatcherAssert.assertThat(pair.getTargetClass(), IsEqual.equalTo(String.class));
	}

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, MappingTypePair> DESC_TO_PAIR_MAP = new HashMap<String, MappingTypePair>() {
		{
			this.put("default", new MappingTypePair(Integer.class, String.class));
			this.put("diff-source", new MappingTypePair(Long.class, String.class));
			this.put("diff-target", new MappingTypePair(Integer.class, Long.class));
			this.put("reversed-types", new MappingTypePair(String.class, Integer.class));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MappingTypePair pair = new MappingTypePair(Integer.class, String.class);

		// Assert:
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("default"), IsEqual.equalTo(pair));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("diff-source"), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("diff-target"), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("reversed-types"), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(Integer.class, IsNot.not(IsEqual.equalTo((Object) pair)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new MappingTypePair(Integer.class, String.class).hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("diff-source").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("diff-target").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_PAIR_MAP.get("reversed-types").hashCode(), IsEqual.equalTo(hashCode));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final MappingTypePair pair = new MappingTypePair(Integer.class, String.class);

		// Assert:
		MatcherAssert.assertThat(pair.toString(), IsEqual.equalTo("java.lang.Integer -> java.lang.String"));
	}

	// endregion
}
