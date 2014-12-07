package org.nem.core.i18n;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.ResourceBundle;

public class UTF8ResourceBundleControlTest {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("test", new UTF8ResourceBundleControl());

	@Test
	public void canLoadAsciiResources() {
		// Act:
		final String str = BUNDLE.getString("Ascii");

		// Assert:
		Assert.assertThat(str, IsEqual.equalTo("something simple"));
	}

	@Test
	public void canLoadUtf8Resources() {
		// Act:
		final String str = BUNDLE.getString("Utf8");

		// Assert:
		Assert.assertThat(str, IsEqual.equalTo("something with german umlaut: äöü"));
	}
}