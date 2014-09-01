package org.nem.core.model.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TimeOffsetTest {
	//region constructor

	@Test
	public void canBeCreatedFromLongValue() {
		// Act:
		final TimeOffset offset = new TimeOffset(1234);

		// Assert:
		Assert.assertThat(offset.getRaw(), IsEqual.equalTo(1234L));
	}

	@Test
	public void canBeCreatedFromNegativeLongValue() {
		// Act:
		final TimeOffset offset = new TimeOffset(-1234);

		// Assert:
		Assert.assertThat(offset.getRaw(), IsEqual.equalTo(-1234L));
	}

	//endregion

	//region add/subtract

	@Test
	public void timeOffsetsCanBeAdded() {
		// Arrange:
		final TimeOffset offset1 = new TimeOffset(1234);
		final TimeOffset offset2 = new TimeOffset(579);

		// Act + Assert:
		Assert.assertThat(offset1.add(offset2), IsEqual.equalTo(new TimeOffset(1813L)));
		Assert.assertThat(offset2.add(offset1), IsEqual.equalTo(new TimeOffset(1813L)));
	}

	@Test
	public void timeOffsetsCanBeSubtracted() {
		// Arrange:
		final TimeOffset offset1 = new TimeOffset(1234);
		final TimeOffset offset2 = new TimeOffset(579);

		// Act + Assert:
		Assert.assertThat(offset1.subtract(offset2), IsEqual.equalTo(new TimeOffset(655L)));
		Assert.assertThat(offset2.subtract(offset1), IsEqual.equalTo(new TimeOffset(-655L)));
	}

	//endregion
}
