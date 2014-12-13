package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;

public class AccountInfoTest {

	@Test
	public void infoInfoCanBeCreated() {
		// Act:
		final ReadOnlyAccountInfo info = new AccountInfo();

		// Assert:
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(info.getHarvestedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(info.getReferenceCount(), IsEqual.equalTo(ReferenceCount.ZERO));
		Assert.assertThat(info.getLabel(), IsNull.nullValue());
	}

	//region Label

	@Test
	public void labelCanBeSet() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.setLabel("Beta Gamma");

		// Assert:
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("Beta Gamma"));
	}

	//endregion

	//region Balance

	@Test
	public void balanceCanBeIncremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementBalance(new Amount(7));

		// Assert:
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(new Amount(7)));
	}

	@Test
	public void balanceCanBeDecremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementBalance(new Amount(100));
		info.decrementBalance(new Amount(12));

		// Assert:
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(new Amount(88)));
	}

	@Test
	public void balanceCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementBalance(new Amount(100));
		info.decrementBalance(new Amount(12));
		info.incrementBalance(new Amount(22));
		info.decrementBalance(new Amount(25));

		// Assert:
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(new Amount(85)));
	}

	//endregion

	//region refCount

	@Test
	public void referenceCountCanBeIncremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		final ReferenceCount result = info.incrementReferenceCount();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(1)));
		Assert.assertThat(info.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(1)));
	}

	@Test
	public void referenceCountCanBeDecremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();
		info.incrementReferenceCount();

		// Act:
		final ReferenceCount result = info.decrementReferenceCount();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(0)));
		Assert.assertThat(info.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(0)));
	}

	//endregion

	//region harvested blocks

	@Test
	public void harvestedBlocksCanBeIncremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();

		// Assert:
		Assert.assertThat(info.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void harvestedBlocksCanBeDecremented() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.decrementHarvestedBlocks();

		// Assert:
		Assert.assertThat(info.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(1)));
	}

	@Test
	public void harvestedBlocksCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.decrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.decrementHarvestedBlocks();

		// Assert:
		Assert.assertThat(info.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesDeepCopy() {
		// Arrange:
		final AccountInfo info = new AccountInfo();
		info.incrementBalance(Amount.fromNem(1000));
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.incrementHarvestedBlocks();
		info.setLabel("Alpha Sigma");
		info.incrementReferenceCount();
		info.incrementReferenceCount();

		// Act:
		final ReadOnlyAccountInfo copy = info.copy();

		// Assert:
		Assert.assertThat(copy, IsNot.not(IsSame.sameInstance(info)));
		Assert.assertThat(copy.getBalance(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(copy.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(copy.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(2)));
		Assert.assertThat(copy.getLabel(), IsEqual.equalTo("Alpha Sigma"));
	}

	//endregion
}