package org.nem.core.serialization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;

public class DeserializationContextTest {

	@Test
	public void findAccountByAddressDelegatesToLookup() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final SimpleAccountLookup lookup = Mockito.mock(SimpleAccountLookup.class);
		Mockito.when(lookup.findByAddress(Mockito.any())).thenReturn(account);
		final DeserializationContext context = new DeserializationContext(lookup);

		// Act:
		final Account foundAccount = context.findAccountByAddress(account.getAddress());

		// Assert:
		MatcherAssert.assertThat(foundAccount, IsEqual.equalTo(account));
		Mockito.verify(lookup, Mockito.only()).findByAddress(account.getAddress());
	}

	@Test
	public void contextConstantsAreInitializedCorrectly() {
		// Arrange:
		final SimpleAccountLookup lookup = Mockito.mock(SimpleAccountLookup.class);
		final DeserializationContext context = new DeserializationContext(lookup);

		// Assert:
		MatcherAssert.assertThat(context.getDefaultMaxBytesLimit(), IsEqual.equalTo(2048));
		MatcherAssert.assertThat(context.getDefaultMaxCharsLimit(), IsEqual.equalTo(128));
	}
}
