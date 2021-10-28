package org.nem.nis.dbmodel;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;

import java.util.*;

public class DbMosaicIdTest {

	// region ctor

	@Test
	public void canCreateDbMosaicId() {
		// Act:
		final DbMosaicId dbMosaicId = new DbMosaicId(123L);

		// Assert:
		MatcherAssert.assertThat(dbMosaicId.getId(), IsEqual.equalTo(123L));
	}

	// endregion

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, DbMosaicId> DESC_TO_DB_MOSAIC_ID_MAP = new HashMap<String, DbMosaicId>() {
		{
			this.put("default", createDbMosaicId(1L));
			this.put("copy", createDbMosaicId(1L));
			this.put("diff-id", createDbMosaicId(2L));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final DbMosaicId dbMosaicId = DESC_TO_DB_MOSAIC_ID_MAP.get("default");

		// Assert:
		final List<String> differentKeys = Collections.singletonList("diff-id");
		for (final Map.Entry<String, DbMosaicId> entry : DESC_TO_DB_MOSAIC_ID_MAP.entrySet()) {
			final Matcher<DbMosaicId> matcher = differentKeys.contains(entry.getKey())
					? IsNot.not(IsEqual.equalTo(dbMosaicId))
					: IsEqual.equalTo(dbMosaicId);

			MatcherAssert.assertThat(entry.getValue(), matcher);
		}
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = DESC_TO_DB_MOSAIC_ID_MAP.get("default").hashCode();

		// Assert:
		final List<String> differentKeys = Collections.singletonList("diff-id");
		for (final Map.Entry<String, DbMosaicId> entry : DESC_TO_DB_MOSAIC_ID_MAP.entrySet()) {
			final Matcher<Integer> matcher = differentKeys.contains(entry.getKey())
					? IsNot.not(IsEqual.equalTo(hashCode))
					: IsEqual.equalTo(hashCode);

			MatcherAssert.assertThat(entry.getValue().hashCode(), matcher);
		}
	}

	// endregion

	private static DbMosaicId createDbMosaicId(final Long id) {
		return new DbMosaicId(id);
	}
}
