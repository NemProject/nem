package org.nem.nis.controller.interceptors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.ExceptionAssert;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.RemoteLinkFactory;
import org.springframework.validation.*;

import javax.servlet.http.HttpServletRequest;

public class InsecurePrivateKeyValidatorTest {

	// region supports

	@Test
	public void privateKeyValidationIsSupported() {
		// Arrange:
		final Validator validator = new TestContext().validator;

		// Act:
		final boolean isSupported = validator.supports(PrivateKey.class);

		// Assert:
		MatcherAssert.assertThat(isSupported, IsEqual.equalTo(true));
	}

	@Test
	public void otherClassValidationIsNotSupported() {
		// Arrange:
		final Validator validator = new TestContext().validator;

		// Act:
		final boolean isSupported = validator.supports(PublicKey.class);

		// Assert:
		MatcherAssert.assertThat(isSupported, IsEqual.equalTo(false));
	}

	// endregion

	// region validate

	@Test
	public void localRequestWithRemoteHarvesterKeyIsAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setLocalRequest(true);
		context.setRemoteHarvester();

		// Assert: no exception
		context.validate();
	}

	@Test
	public void localRequestWithNonRemoteHarvesterKeyIsAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setLocalRequest(true);

		// Assert: no exception
		context.validate();
	}

	@Test
	public void remoteRequestWithRemoteHarvesterKeyIsAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setLocalRequest(false);
		context.setRemoteHarvester();

		// Assert: no exception
		context.validate();
	}

	@Test
	public void remoteRequestWithNonRemoteHarvesterKeyIsAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setLocalRequest(false);

		// Assert:
		ExceptionAssert.assertThrows(v -> context.validate(), UnauthorizedAccessException.class);
	}

	// endregion

	private static class TestContext {
		private final KeyPair keyPair = new KeyPair();
		private final Address address = Address.fromPublicKey(this.keyPair.getPublicKey());
		private final AccountState accountState = new AccountState(this.address);

		private final LocalHostDetector localHostDetector = Mockito.mock(LocalHostDetector.class);
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		private final Validator validator = new InsecurePrivateKeyValidator(this.localHostDetector, this.accountStateCache, this.request);

		public TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(this.accountState);
		}

		public void setLocalRequest(final boolean isLocal) {
			Mockito.when(this.localHostDetector.isLocal(this.request)).thenReturn(isLocal);
		}

		public void setRemoteHarvester() {
			this.accountState.getRemoteLinks().addLink(RemoteLinkFactory.activateRemoteHarvester(this.address, BlockHeight.ONE));
		}

		public void validate() {
			this.validator.validate(this.keyPair.getPrivateKey(), Mockito.mock(Errors.class));
		}
	}
}
