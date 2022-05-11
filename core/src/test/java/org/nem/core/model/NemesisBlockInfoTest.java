package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class NemesisBlockInfoTest {

	@Test
	public void canCreateNemesisBlock() {
		// Arrange:
		final Hash generationHash = Utils.generateRandomHash();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final NemesisBlockInfo info = new NemesisBlockInfo(generationHash, address, Amount.fromNem(44221122), "awesome-nemesis.bin");

		// Assert:
		MatcherAssert.assertThat(info.getGenerationHash(), IsEqual.equalTo(generationHash));
		MatcherAssert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(info.getAmount(), IsEqual.equalTo(Amount.fromNem(44221122)));
		MatcherAssert.assertThat(info.getDataFileName(), IsEqual.equalTo("awesome-nemesis.bin"));
	}
}
