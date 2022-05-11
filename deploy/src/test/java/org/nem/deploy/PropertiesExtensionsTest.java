package org.nem.deploy;

import org.hamcrest.core.IsEqual;
import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class PropertiesExtensionsTest {

	// region merge

	@Test
	public void canMergeZeroProperties() {
		// Act:
		final Properties mergedProps = PropertiesExtensions.merge(Collections.emptyList());

		// Assert:
		MatcherAssert.assertThat(mergedProps.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canMergeSingleProperties() {
		// Arrange:
		final Properties baseProps = new Properties();
		baseProps.setProperty("alpha", "one");
		baseProps.setProperty("beta", "two");
		baseProps.setProperty("gamma", "three");

		// Act:
		final Properties mergedProps = PropertiesExtensions.merge(Collections.singletonList(baseProps));

		// Assert:
		// - baseProps was not modified
		// - baseProps was copied to mergedProps modified
		for (final Properties props : Arrays.asList(baseProps, mergedProps)) {
			MatcherAssert.assertThat(props.size(), IsEqual.equalTo(3));
			MatcherAssert.assertThat(props.getProperty("alpha"), IsEqual.equalTo("one"));
			MatcherAssert.assertThat(props.getProperty("beta"), IsEqual.equalTo("two"));
			MatcherAssert.assertThat(props.getProperty("gamma"), IsEqual.equalTo("three"));
		}
	}

	@Test
	public void canMergeMultipleProperties() {
		// Arrange:
		final Properties baseProps = new Properties();
		baseProps.setProperty("alpha", "one");
		baseProps.setProperty("beta", "two");
		baseProps.setProperty("gamma", "three");

		final Properties overrideProps1 = new Properties();
		overrideProps1.setProperty("beta", ":/");
		overrideProps1.setProperty("radiation", "bad");
		overrideProps1.setProperty("one", "z");

		final Properties overrideProps2 = new Properties();
		overrideProps2.setProperty("alpha", "zero");
		overrideProps2.setProperty("two", "2");
		overrideProps2.setProperty("one", "1");

		// Act:
		final Properties mergedProps = PropertiesExtensions.merge(Arrays.asList(baseProps, overrideProps1, overrideProps2));

		// Assert:
		// - baseProps was not modified
		MatcherAssert.assertThat(baseProps.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(baseProps.getProperty("alpha"), IsEqual.equalTo("one"));
		MatcherAssert.assertThat(baseProps.getProperty("beta"), IsEqual.equalTo("two"));
		MatcherAssert.assertThat(baseProps.getProperty("gamma"), IsEqual.equalTo("three"));

		// - overrideProps1 was not modified
		MatcherAssert.assertThat(overrideProps1.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(overrideProps1.getProperty("beta"), IsEqual.equalTo(":/"));
		MatcherAssert.assertThat(overrideProps1.getProperty("radiation"), IsEqual.equalTo("bad"));
		MatcherAssert.assertThat(overrideProps1.getProperty("one"), IsEqual.equalTo("z"));

		// - overrideProps2 was not modified
		MatcherAssert.assertThat(overrideProps2.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(overrideProps2.getProperty("alpha"), IsEqual.equalTo("zero"));
		MatcherAssert.assertThat(overrideProps2.getProperty("two"), IsEqual.equalTo("2"));
		MatcherAssert.assertThat(overrideProps2.getProperty("one"), IsEqual.equalTo("1"));

		// - mergedProps contains baseProps > overrideProps1 > overrideProps2 and precedence is right to left
		MatcherAssert.assertThat(mergedProps.size(), IsEqual.equalTo(6));
		MatcherAssert.assertThat(mergedProps.getProperty("alpha"), IsEqual.equalTo("zero"));
		MatcherAssert.assertThat(mergedProps.getProperty("beta"), IsEqual.equalTo(":/"));
		MatcherAssert.assertThat(mergedProps.getProperty("gamma"), IsEqual.equalTo("three"));
		MatcherAssert.assertThat(mergedProps.getProperty("radiation"), IsEqual.equalTo("bad"));
		MatcherAssert.assertThat(mergedProps.getProperty("one"), IsEqual.equalTo("1"));
		MatcherAssert.assertThat(mergedProps.getProperty("two"), IsEqual.equalTo("2"));
	}

	// endregion

	// region loadFromResource

	@Test
	public void loadFromResourceCanLoadRequiredProperties() {
		// Act:
		final Properties properties = loadFromResource("test.properties", true);

		// Assert:
		MatcherAssert.assertThat(properties.size(), IsEqual.equalTo(2));
	}

	@Test
	public void loadFromResourceThrowsExceptionWhenCannotLoadRequiredResources() {
		// Act:
		ExceptionAssert.assertThrows(v -> loadFromResource("imaginary.properties", true), IllegalArgumentException.class);
	}

	@Test
	public void loadFromResourceCanLoadOptionalProperties() {
		// Act:
		final Properties properties = loadFromResource("test.properties", false);

		// Assert:
		MatcherAssert.assertThat(properties.size(), IsEqual.equalTo(2));
	}

	@Test
	public void loadFromResourceReturnsEmptyResourcesWhenCannotLoadOptionalResources() {
		// Act:
		final Properties properties = loadFromResource("imaginary.properties", false);

		// Assert:
		MatcherAssert.assertThat(properties.size(), IsEqual.equalTo(0));
	}

	private static Properties loadFromResource(final String name, final boolean isRequired) {
		return PropertiesExtensions.loadFromResource(PropertiesExtensionsTest.class, name, isRequired);
	}

	// endregion
}
