package org.nem.nis.connect;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.peer.connect.Communicator;

import java.net.*;
import java.util.concurrent.*;

public class AuditedCommunicatorTest {

	@Test
	public void postDelegatesToInnerCommunicator() throws MalformedURLException {
		// Arrange:
		final URL url = new URL("http://localhost/my/path");
		final TestRunner context = new PostTestRunner();
		Mockito.when(context.post(context.innerCommunicator, url, context.entity)).thenReturn(new CompletableFuture<>());

		// Act:
		context.post(context.communicator, url, context.entity);

		// Assert:
		Mockito.verify(context.innerCommunicator, Mockito.times(1)).post(url, context.entity);
	}

	@Test
	public void postAddsToAuditCollection() throws MalformedURLException {
		// Assert:
		new PostTestRunner().assertPostAddsToAuditCollection();
	}

	@Test
	public void postSuccessfulCompletionRemovesFromAuditCollection() throws MalformedURLException {
		// Assert:
		new PostTestRunner().assertPostSuccessfulCompletionRemovesFromAuditCollection();
	}

	@Test
	public void postExceptionalCompletionRemovesFromAuditCollection() throws MalformedURLException {
		// Assert:
		new PostTestRunner().assertPostExceptionalCompletionRemovesFromAuditCollection();
	}

	@Test
	public void postVoidDelegatesToInnerCommunicator() throws MalformedURLException {
		// Arrange:
		final URL url = new URL("http://localhost/my/path");
		final TestRunner context = new PostVoidTestRunner();
		Mockito.when(context.post(context.innerCommunicator, url, context.entity)).thenReturn(new CompletableFuture<>());

		// Act:
		context.post(context.communicator, url, context.entity);

		// Assert:
		Mockito.verify(context.innerCommunicator, Mockito.times(1)).postVoid(url, context.entity);
	}

	@Test
	public void postVoidAddsToAuditCollection() throws MalformedURLException {
		// Assert:
		new PostVoidTestRunner().assertPostAddsToAuditCollection();
	}

	@Test
	public void postVoidSuccessfulCompletionRemovesFromAuditCollection() throws MalformedURLException {
		// Assert:
		new PostVoidTestRunner().assertPostSuccessfulCompletionRemovesFromAuditCollection();
	}

	@Test
	public void postVoidExceptionalCompletionRemovesFromAuditCollection() throws MalformedURLException {
		// Assert:
		new PostVoidTestRunner().assertPostExceptionalCompletionRemovesFromAuditCollection();
	}

	private static abstract class TestRunner {
		private final Communicator innerCommunicator = Mockito.mock(Communicator.class);
		private final AuditCollection collection = Mockito.mock(AuditCollection.class);
		private final AuditedCommunicator communicator = new AuditedCommunicator(this.innerCommunicator, this.collection);
		private final SerializableEntity entity = new MockSerializableEntity();
		private final Deserializer deserializer = Mockito.mock(Deserializer.class);

		protected abstract CompletableFuture<Deserializer> post(final Communicator communicator, final URL url,
				final SerializableEntity entity);

		public void assertPostAddsToAuditCollection() throws MalformedURLException {
			// Arrange:
			final URL url = new URL("http://localhost/my/path");
			Mockito.when(this.post(this.innerCommunicator, url, this.entity)).thenReturn(new CompletableFuture<>());

			// Act:
			this.post(this.communicator, url, this.entity);

			// Assert:
			Mockito.verify(this.collection, Mockito.times(1)).add("localhost", "/my/path");
		}

		public void assertPostSuccessfulCompletionRemovesFromAuditCollection() throws MalformedURLException {
			// Arrange:
			final URL url = new URL("http://localhost/my/path");
			Mockito.when(this.post(this.innerCommunicator, url, this.entity))
					.thenReturn(CompletableFuture.completedFuture(this.deserializer));

			// Act:
			final Deserializer deserializer = this.post(this.communicator, url, this.entity).join();

			// Assert:
			Mockito.verify(this.collection, Mockito.times(1)).remove("localhost", "/my/path");
			MatcherAssert.assertThat(deserializer, IsSame.sameInstance(this.deserializer));
		}

		public void assertPostExceptionalCompletionRemovesFromAuditCollection() throws MalformedURLException {
			// Arrange:
			final URL url = new URL("http://localhost/my/path");
			final CompletableFuture<Deserializer> future = new CompletableFuture<>();
			future.completeExceptionally(new RuntimeException());
			Mockito.when(this.post(this.innerCommunicator, url, this.entity)).thenReturn(future);

			// Assert:
			ExceptionAssert.assertThrows(v -> this.post(this.communicator, url, this.entity).join(), CompletionException.class);

			// Assert:
			Mockito.verify(this.collection, Mockito.times(1)).remove("localhost", "/my/path");
		}
	}

	private static class PostTestRunner extends TestRunner {
		@Override
		protected CompletableFuture<Deserializer> post(final Communicator communicator, final URL url, final SerializableEntity entity) {
			return communicator.post(url, entity);
		}
	}

	private static class PostVoidTestRunner extends TestRunner {
		@Override
		protected CompletableFuture<Deserializer> post(final Communicator communicator, final URL url, final SerializableEntity entity) {
			return communicator.postVoid(url, entity);
		}
	}
}
