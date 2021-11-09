package org.nem.core.utils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class StringUtilsTest {

	@Test
	public void isNullOrEmptyReturnsCorrectResult() {
		// Assert:
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty(null), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty(""), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty("   "), IsEqual.equalTo(false));
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty(" \t  \t"), IsEqual.equalTo(false));
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty("foo"), IsEqual.equalTo(false));
		MatcherAssert.assertThat(StringUtils.isNullOrEmpty(" foo "), IsEqual.equalTo(false));
	}

	@Test
	public void isNullOrWhitespaceReturnsCorrectResult() {
		// Assert:
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace(null), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace(""), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace("   "), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace(" \t  \t"), IsEqual.equalTo(true));
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace("foo"), IsEqual.equalTo(false));
		MatcherAssert.assertThat(StringUtils.isNullOrWhitespace(" foo "), IsEqual.equalTo(false));
	}

	@Test
	public void replaceVariableOnStringWithoutVariablesReturnsStringItself() {
		// Assert:
		MatcherAssert.assertThat(StringUtils.replaceVariable("quick brown fox", "variable", "-"), IsEqual.equalTo("quick brown fox"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("", "variable", "-"), IsEqual.equalTo(""));
		MatcherAssert.assertThat(StringUtils.replaceVariable("variable", "variable", "-"), IsEqual.equalTo("variable"));
	}

	@Test
	public void replaceVariableReplaceOnlyExactVariables() {
		MatcherAssert.assertThat(StringUtils.replaceVariable("${   }", " ", "-"), IsEqual.equalTo("${   }"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("${ foo}", "foo", "-"), IsEqual.equalTo("${ foo}"));
	}

	@Test
	public void replaceVariableOnStringWithVariablesReturnsCorrectResults() {
		MatcherAssert.assertThat(StringUtils.replaceVariable("${variable}", "variable", "-"), IsEqual.equalTo("-"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("${ }", " ", "-"), IsEqual.equalTo("-"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("${    }", "    ", "-"), IsEqual.equalTo("-"));
	}

	@Test
	public void replaceVariableMustMatchVariableCaseSensitively() {
		// Assert:
		MatcherAssert.assertThat(StringUtils.replaceVariable("${Variable}", "variable", "-"), IsEqual.equalTo("${Variable}"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("${Variable}", "xx", "-"), IsEqual.equalTo("${Variable}"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("${Variable}", "", "-"), IsEqual.equalTo("${Variable}"));
	}

	@Test
	public void replaceVariableCanReplaceVariableOccurrencesBetweenText() {
		MatcherAssert.assertThat(StringUtils.replaceVariable("quick ${color} fox", "color", "brown"), IsEqual.equalTo("quick brown fox"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("jumps over the ${adj} dog", "adj", "lazy"),
				IsEqual.equalTo("jumps over the lazy dog"));
	}

	@Test
	public void replaceVariableCanReplaceMultipleOccurrencesOfVariable() {
		MatcherAssert.assertThat(StringUtils.replaceVariable("quick ${color} ${color} fox", "color", "brown"),
				IsEqual.equalTo("quick brown brown fox"));
		MatcherAssert.assertThat(StringUtils.replaceVariable("Buffalo ${} Buffalo ${} ${} ${} Buffalo ${}", "", "buffalo"),
				IsEqual.equalTo("Buffalo buffalo Buffalo buffalo buffalo buffalo Buffalo buffalo"));
	}
}
