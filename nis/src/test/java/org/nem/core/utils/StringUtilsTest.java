package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class StringUtilsTest {

	@Test
	public void isNullOrEmptyReturnsCorrectResult() {
		// Assert:
		Assert.assertThat(StringUtils.isNullOrEmpty(null), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrEmpty(""), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrEmpty("   "), IsEqual.equalTo(false));
		Assert.assertThat(StringUtils.isNullOrEmpty(" \t  \t"), IsEqual.equalTo(false));
		Assert.assertThat(StringUtils.isNullOrEmpty("foo"), IsEqual.equalTo(false));
		Assert.assertThat(StringUtils.isNullOrEmpty(" foo "), IsEqual.equalTo(false));
	}

	@Test
	public void isNullOrWhitespaceReturnsCorrectResult() {
		// Assert:
		Assert.assertThat(StringUtils.isNullOrWhitespace(null), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrWhitespace(""), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrWhitespace("   "), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrWhitespace(" \t  \t"), IsEqual.equalTo(true));
		Assert.assertThat(StringUtils.isNullOrWhitespace("foo"), IsEqual.equalTo(false));
		Assert.assertThat(StringUtils.isNullOrWhitespace(" foo "), IsEqual.equalTo(false));
	}
}
