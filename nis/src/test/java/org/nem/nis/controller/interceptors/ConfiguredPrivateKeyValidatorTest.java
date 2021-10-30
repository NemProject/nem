package org.nem.nis.controller.interceptors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;
import org.springframework.validation.*;

public class ConfiguredPrivateKeyValidatorTest {

	// region supports

	@Test
	public void privateKeyValidationIsSupported() {
		// Arrange:
		final Validator validator = createAllowAllValidator();

		// Act:
		final boolean isSupported = validator.supports(PrivateKey.class);

		// Assert:
		MatcherAssert.assertThat(isSupported, IsEqual.equalTo(true));
	}

	@Test
	public void otherClassValidationIsNotSupported() {
		// Arrange:
		final Validator validator = createAllowAllValidator();

		// Act:
		final boolean isSupported = validator.supports(PublicKey.class);

		// Assert:
		MatcherAssert.assertThat(isSupported, IsEqual.equalTo(false));
	}

	// endregion

	// region validate

	@Test
	public void anyPrivateKeyIsAllowedWithAllowAllConfiguration() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Validator validator = createAllowAllValidator();

		// Assert: no exception
		validate(validator, keyPair.getPrivateKey());
	}

	@Test
	public void privateKeyWithMatchingAddressIsAllowedWithAllowSpecificConfiguration() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Address[] allowedAddresses = {
				Utils.generateRandomAddress(), Address.fromPublicKey(keyPair.getPublicKey()), Utils.generateRandomAddress(),
		};
		final Validator validator = createAllowSpecificValidator(allowedAddresses);

		// Assert: no exception
		validate(validator, keyPair.getPrivateKey());
	}

	@Test
	public void privateKeyWithNonMatchingAddressIsNotAllowedWithAllowSpecificConfiguration() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Address[] allowedAddresses = {
				Utils.generateRandomAddress(), Utils.generateRandomAddress(), Utils.generateRandomAddress(),
		};
		final Validator validator = createAllowSpecificValidator(allowedAddresses);

		// Assert:
		ExceptionAssert.assertThrows(v -> validate(validator, keyPair.getPrivateKey()), UnauthorizedAccessException.class);
	}

	// endregion

	private static Validator createAllowAllValidator() {
		return new ConfiguredPrivateKeyValidator(new Address[]{});
	}

	private static Validator createAllowSpecificValidator(final Address[] allowedAddresses) {
		return new ConfiguredPrivateKeyValidator(allowedAddresses);
	}

	private static void validate(final Validator validator, final PrivateKey privateKey) {
		validator.validate(privateKey, Mockito.mock(Errors.class));
	}
}
