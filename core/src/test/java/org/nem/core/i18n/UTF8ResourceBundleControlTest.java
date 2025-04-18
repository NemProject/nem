package org.nem.core.i18n;

import java.util.ResourceBundle;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class UTF8ResourceBundleControlTest {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("test", new UTF8ResourceBundleControl());

	@Test
	public void canLoadAsciiResources() {
		// Act:
		final String str = BUNDLE.getString("Ascii");

		// Assert:
		MatcherAssert.assertThat(str, IsEqual.equalTo("something simple"));
	}

	@Test
	public void canLoadUtf8Resources() {
		// Act:
		final String str = BUNDLE.getString("Utf8");

		// Assert:
		MatcherAssert.assertThat(str, IsEqual.equalTo("something with german umlaut: äöü"));
	}
}
