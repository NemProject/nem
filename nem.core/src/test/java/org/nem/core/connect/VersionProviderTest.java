package org.nem.core.connect;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.Assert;import org.junit.Test;import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;import org.nem.core.connect.HttpMethodClient;import org.nem.core.node.NodeVersion;
import org.nem.core.utils.ExceptionUtils;

import java.lang.IllegalStateException;import java.lang.SuppressWarnings;import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class VersionProviderTest {

	@Test
	public void getLocalVersionReturnsCorrectVersion() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NodeVersion version = context.provider.getLocalVersion();

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(new NodeVersion(0, 6, 0, "DEVELOPER BUILD")));
	}

	@Test
	public void getLatestVersionDelegatesToHttpClientAndReturnsCorrectVersionOnSuccess() {
		// Arrange:
		final TestContext context = new TestContext();
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("stable", "1.2.3");
		context.setHttpResult(new ErrorResponseDeserializerUnion(200, jsonObject, null));

		// Act:
		final NodeVersion version = context.provider.getLatestVersion();

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(new NodeVersion(1, 2, 3)));
		context.assertDelegationToVersionProvider();
	}

	@Test
	public void getLatestVersionReturnsZeroOnHttpError() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("stable", "1.2.3");

		// Assert:
		assertLatestVersionFailure(new ErrorResponseDeserializerUnion(500, jsonObject, null));
	}

	@Test
	public void getLatestVersionReturnsZeroOnInvalidJsonResponse() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();

		// Assert:
		assertLatestVersionFailure(new ErrorResponseDeserializerUnion(500, jsonObject, null));
	}

	@Test
	public void getLatestVersionReturnsZeroOnAnyOtherFailure() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.client.get(Mockito.any(), Mockito.any())).thenThrow(new IllegalStateException("other error"));

		// Act:
		final NodeVersion version = context.provider.getLatestVersion();

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(NodeVersion.ZERO));
		context.assertDelegationToVersionProvider();
	}

	private static void assertLatestVersionFailure(final ErrorResponseDeserializerUnion result) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setHttpResult(result);

		// Act:
		final NodeVersion version = context.provider.getLatestVersion();

		// Assert:
		Assert.assertThat(version, IsEqual.equalTo(NodeVersion.ZERO));
		context.assertDelegationToVersionProvider();
	}

	private static class TestContext {
		@SuppressWarnings("unchecked")
		private final HttpMethodClient<ErrorResponseDeserializerUnion> client = Mockito.mock(HttpMethodClient.class);
		private final VersionProvider provider = new VersionProvider(this.client);

		public void setHttpResult(final ErrorResponseDeserializerUnion result) {
			@SuppressWarnings("unchecked")
			final HttpMethodClient.AsyncToken<ErrorResponseDeserializerUnion> token = Mockito.mock(HttpMethodClient.AsyncToken.class);
			Mockito.when(token.getFuture())
					.thenReturn(CompletableFuture.completedFuture(result));
			Mockito.when(this.client.get(Mockito.any(), Mockito.any())).thenReturn(token);
		}

		public void assertDelegationToVersionProvider() {
			final URL versionProviderUrl = ExceptionUtils.propagate(() -> new URL("http://bob.nem.ninja/version.json"));
			Mockito.verify(this.client, Mockito.only()).get(Mockito.eq(versionProviderUrl), Mockito.any());
		}
	}
}