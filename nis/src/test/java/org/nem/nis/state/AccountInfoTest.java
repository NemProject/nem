package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.Collectors;

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
		Assert.assertThat(info.getMosaicIds().isEmpty(), IsEqual.equalTo(true));
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

	//region mosaic ids

	@Test
	public void mosaicIdsCanBeAdded() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.addMosaicId(Utils.createMosaicId(1));
		info.addMosaicId(Utils.createMosaicId(3));
		info.addMosaicId(Utils.createMosaicId(1));
		info.addMosaicId(Utils.createMosaicId(7));

		// Assert:
		Assert.assertThat(info.getMosaicIds(), IsEquivalent.equivalentTo(getMosaicIds(1, 3, 7)));
	}

	@Test
	public void mosaicIdsCanBeRemoved() {
		// Arrange:
		final AccountInfo info = new AccountInfo();

		// Act:
		info.addMosaicId(Utils.createMosaicId(1));
		info.addMosaicId(Utils.createMosaicId(3));
		info.addMosaicId(Utils.createMosaicId(7));
		info.removeMosaicId(Utils.createMosaicId(3));

		// Assert:
		Assert.assertThat(info.getMosaicIds(), IsEquivalent.equivalentTo(getMosaicIds(1, 7)));
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

	@Test
	public void copyCreatesDeepCopyOfMosaicIds() {
		// Arrange:
		final AccountInfo info = new AccountInfo();
		info.addMosaicId(Utils.createMosaicId(1));
		info.addMosaicId(Utils.createMosaicId(3));
		info.addMosaicId(Utils.createMosaicId(7));
		info.removeMosaicId(Utils.createMosaicId(3));

		// Act:
		final ReadOnlyAccountInfo copy = info.copy();
		info.removeMosaicId(Utils.createMosaicId(1));

		// Assert:
		// - the original mosaics were copied
		// - 1 was only removed from the original info
		Assert.assertThat(copy.getMosaicIds(), IsEquivalent.equivalentTo(getMosaicIds(1, 7)));
		Assert.assertThat(info.getMosaicIds(), IsEquivalent.equivalentTo(getMosaicIds(7)));
	}

	//endregion

	private static Collection<MosaicId> getMosaicIds(final int... ids) {
		return Arrays.stream(ids).mapToObj(Utils::createMosaicId).collect(Collectors.toList());
	}
}