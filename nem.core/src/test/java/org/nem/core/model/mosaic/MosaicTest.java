package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.test.*;

import java.util.Properties;

public class MosaicTest {

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final Properties properties = createProperties();

		// Act:
		final Mosaic mosaic = new Mosaic(creator, properties);

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getName(), IsEqual.equalTo("Alice's gift vouchers"));
	}

	@Test
	public void cannotCreateMosaicWithNullCreator() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(null, createProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), null), IllegalArgumentException.class);
	}

	private static Properties createProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return properties;
	}
}
