package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

public class GroupedHeightTest {

	@SuppressWarnings("serial")
	private static final Map<Integer, Integer> HEIGHT_TO_GROUPED_HEIGHT_MAP = new HashMap<Integer, Integer>() {
		{
			this.put(1, 1);
			this.put(358, 1);
			this.put(359, 1);
			this.put(360, 359);
			this.put(361, 359);
			this.put(1074, 718);
			this.put(1095, 1077);
		}
	};

	@Test
	public void fromHeightReturnsGroupedBlockHeight() {
		// Assert:
		for (final Map.Entry<Integer, Integer> pair : HEIGHT_TO_GROUPED_HEIGHT_MAP.entrySet()) {
			assertGroupedHeight(pair.getKey(), pair.getValue());
		}
	}

	private static void assertGroupedHeight(final long height, final long expectedGroupedHeight) {
		// Act:
		final BlockHeight groupedHeight = GroupedHeight.fromHeight(new BlockHeight(height));

		// Assert:
		MatcherAssert.assertThat(groupedHeight, IsEqual.equalTo(new BlockHeight(expectedGroupedHeight)));
	}
}
