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

	@Test
	public void replaceVariableOnStringWithoutVariablesReturnsStringItself() {
		// Assert:
		Assert.assertThat(StringUtils.replaceVariable("quick brown fox", "variable", "-"), IsEqual.equalTo("quick brown fox"));
		Assert.assertThat(StringUtils.replaceVariable("", "variable", "-"), IsEqual.equalTo(""));
		Assert.assertThat(StringUtils.replaceVariable("variable", "variable", "-"), IsEqual.equalTo("variable"));
	}

	@Test
	public void replaceVariableReplaceOnlyExactVariables() {
		Assert.assertThat(StringUtils.replaceVariable("${   }", " ", "-"), IsEqual.equalTo("${   }"));
		Assert.assertThat(StringUtils.replaceVariable("${ foo}", "foo", "-"), IsEqual.equalTo("${ foo}"));
	}

	@Test
	public void replaceVariableOnStringWithVariablesReturnsCorrectResults() {
		Assert.assertThat(StringUtils.replaceVariable("${variable}", "variable", "-"), IsEqual.equalTo("-"));
		Assert.assertThat(StringUtils.replaceVariable("${ }", " ", "-"), IsEqual.equalTo("-"));
		Assert.assertThat(StringUtils.replaceVariable("${    }", "    ", "-"), IsEqual.equalTo("-"));
	}

	@Test
	public void replaceVariableMustMatchVariableCaseSensitively() {
		// Assert:
		Assert.assertThat(StringUtils.replaceVariable("${Variable}", "variable", "-"), IsEqual.equalTo("${Variable}"));
		Assert.assertThat(StringUtils.replaceVariable("${Variable}", "xx", "-"), IsEqual.equalTo("${Variable}"));
		Assert.assertThat(StringUtils.replaceVariable("${Variable}", "", "-"), IsEqual.equalTo("${Variable}"));
	}

	@Test
	public void replaceVariableCanReplaceVariableOccurrencesBetweenText() {
		Assert.assertThat(StringUtils.replaceVariable("quick ${color} fox", "color", "brown"), IsEqual.equalTo("quick brown fox"));
		Assert.assertThat(StringUtils.replaceVariable("jumps over the ${adj} dog", "adj", "lazy"), IsEqual.equalTo("jumps over the lazy dog"));
	}

	@Test
	public void replaceVariableCanReplaceMultipleOccurrencesOfVariable() {
		Assert.assertThat(StringUtils.replaceVariable("quick ${color} ${color} fox", "color", "brown"), IsEqual.equalTo("quick brown brown fox"));
		Assert.assertThat(StringUtils.replaceVariable("Buffalo ${} Buffalo ${} ${} ${} Buffalo ${}", "", "buffalo"),
				IsEqual.equalTo("Buffalo buffalo Buffalo buffalo buffalo buffalo Buffalo buffalo"));
	}
}
