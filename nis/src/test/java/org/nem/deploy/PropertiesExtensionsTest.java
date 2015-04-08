package org.nem.deploy;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class PropertiesExtensionsTest {

	//region merge

	@Test
	public void canMergeZeroProperties() {
		// Act:
		final Properties mergedProps = PropertiesExtensions.merge(Collections.emptyList());

		// Assert:
		Assert.assertThat(mergedProps.size(), IsEqual.equalTo(0));
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
			Assert.assertThat(props.size(), IsEqual.equalTo(3));
			Assert.assertThat(props.getProperty("alpha"), IsEqual.equalTo("one"));
			Assert.assertThat(props.getProperty("beta"), IsEqual.equalTo("two"));
			Assert.assertThat(props.getProperty("gamma"), IsEqual.equalTo("three"));
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
		Assert.assertThat(baseProps.size(), IsEqual.equalTo(3));
		Assert.assertThat(baseProps.getProperty("alpha"), IsEqual.equalTo("one"));
		Assert.assertThat(baseProps.getProperty("beta"), IsEqual.equalTo("two"));
		Assert.assertThat(baseProps.getProperty("gamma"), IsEqual.equalTo("three"));

		// - overrideProps1 was not modified
		Assert.assertThat(overrideProps1.size(), IsEqual.equalTo(3));
		Assert.assertThat(overrideProps1.getProperty("beta"), IsEqual.equalTo(":/"));
		Assert.assertThat(overrideProps1.getProperty("radiation"), IsEqual.equalTo("bad"));
		Assert.assertThat(overrideProps1.getProperty("one"), IsEqual.equalTo("z"));

		// - overrideProps2 was not modified
		Assert.assertThat(overrideProps2.size(), IsEqual.equalTo(3));
		Assert.assertThat(overrideProps2.getProperty("alpha"), IsEqual.equalTo("zero"));
		Assert.assertThat(overrideProps2.getProperty("two"), IsEqual.equalTo("2"));
		Assert.assertThat(overrideProps2.getProperty("one"), IsEqual.equalTo("1"));

		// - mergedProps contains baseProps > overrideProps1 > overrideProps2 and precedence is right to left
		Assert.assertThat(mergedProps.size(), IsEqual.equalTo(6));
		Assert.assertThat(mergedProps.getProperty("alpha"), IsEqual.equalTo("zero"));
		Assert.assertThat(mergedProps.getProperty("beta"), IsEqual.equalTo(":/"));
		Assert.assertThat(mergedProps.getProperty("gamma"), IsEqual.equalTo("three"));
		Assert.assertThat(mergedProps.getProperty("radiation"), IsEqual.equalTo("bad"));
		Assert.assertThat(mergedProps.getProperty("one"), IsEqual.equalTo("1"));
		Assert.assertThat(mergedProps.getProperty("two"), IsEqual.equalTo("2"));
	}

	//endregion

	//region loadFromResource

	@Test
	public void loadFromResourceCanLoadRequiredProperties() {
		// Act:
		final Properties properties = loadFromResource("test.properties", true);

		// Assert:
		Assert.assertThat(properties.size(), IsEqual.equalTo(2));
	}

	@Test
	public void loadFromResourceThrowsExceptionWhenCannotLoadRequiredResources() {
		// Act:
		ExceptionAssert.assertThrows(
				v -> loadFromResource("imaginary.properties", true),
				IllegalArgumentException.class);
	}

	@Test
	public void loadFromResourceCanLoadOptionalProperties() {
		// Act:
		final Properties properties = loadFromResource("test.properties", false);

		// Assert:
		Assert.assertThat(properties.size(), IsEqual.equalTo(2));
	}

	@Test
	public void loadFromResourceReturnsEmptyResourcesWhenCannotLoadOptionalResources() {
		// Act:
		final Properties properties = loadFromResource("imaginary.properties", false);

		// Assert:
		Assert.assertThat(properties.size(), IsEqual.equalTo(0));
	}

	private static Properties loadFromResource(final String name, final boolean isRequired) {
		return PropertiesExtensions.loadFromResource(PropertiesExtensionsTest.class, name, isRequired);
	}

	//endregion
}