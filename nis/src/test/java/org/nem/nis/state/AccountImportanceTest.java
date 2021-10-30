package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.test.*;

import java.util.*;

public class AccountImportanceTest {
	private static final long OUTLINK_HISTORY = NisTestConstants.ESTIMATED_BLOCKS_PER_MONTH;

	// region constructor

	@Test
	public void importanceIsInitiallyUnset() {
		// Arrange:
		final ReadOnlyAccountImportance ai = new AccountImportance();

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(ai.getHeight(), IsNull.nullValue());
	}

	@Test
	public void canCreateAccountImportanceWithParameters() {
		// Arrange:
		final BlockHeight height = new BlockHeight(123);
		final ReadOnlyAccountImportance ai = new AccountImportance(height, 0.234, 0.345);

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(ai.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
		MatcherAssert.assertThat(ai.getImportance(height), IsEqual.equalTo(0.234));
		MatcherAssert.assertThat(ai.getLastPageRank(), IsEqual.equalTo(0.345));
	}

	// endregion

	// region serialization

	@Test
	public void canRoundtripUnsetImportance() {
		// Arrange:
		final ReadOnlyAccountImportance original = new AccountImportance();

		// Act:
		final ReadOnlyAccountImportance ai = roundtripImportance(original);

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(ai.getHeight(), IsNull.nullValue());
	}

	@Test
	public void canRoundtripSetImportance() {
		// Arrange:
		final AccountImportance original = new AccountImportance();
		original.setImportance(new BlockHeight(5), 17);
		original.setLastPageRank(12);

		// Act:
		final ReadOnlyAccountImportance ai = roundtripImportance(original);
		final double importance = ai.getImportance(new BlockHeight(5));

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(importance, IsEqual.equalTo(17.0));
		MatcherAssert.assertThat(ai.getHeight(), IsEqual.equalTo(new BlockHeight(5)));
		MatcherAssert.assertThat(ai.getLastPageRank(), IsEqual.equalTo(12.0));
	}

	private static ReadOnlyAccountImportance roundtripImportance(final ReadOnlyAccountImportance original) {
		return new AccountImportance(Utils.roundtripSerializableEntity(original, null));
	}

	// endregion

	// region outlinks

	@Test
	public void canAddOutlinks() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(NisUtils.createLink(7, 27, "BBB"), NisUtils.createLink(8, 35, "CCC"),
				NisUtils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = this.toList(getOutlinksIterator(ai, new BlockHeight(9)));
		MatcherAssert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		MatcherAssert.assertThat(ai.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(3));
	}

	@Test
	public void canRemoveOutlinks() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.removeOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(NisUtils.createLink(7, 27, "BBB"), NisUtils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = this.toList(getOutlinksIterator(ai, new BlockHeight(9)));
		MatcherAssert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		MatcherAssert.assertThat(ai.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(2));
	}

	@Test
	public void outlinkGettersRespectBlockHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(NisUtils.createLink(7, 27, "BBB"), NisUtils.createLink(8, 35, "CCC"));

		final List<AccountLink> links = this.toList(getOutlinksIterator(ai, new BlockHeight(8)));
		MatcherAssert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		MatcherAssert.assertThat(ai.getOutlinksSize(new BlockHeight(8)), IsEqual.equalTo(2));
	}

	@Test
	public void canPruneOutlinks() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));
		ai.addOutlink(NisUtils.createLink(10, 22, "ZZZ"));

		// Act:
		ai.prune(new BlockHeight(9));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(NisUtils.createLink(9, 18, "AAA"), NisUtils.createLink(10, 22, "ZZZ"));
		final List<AccountLink> links = this.toList(getOutlinksIterator(ai, new BlockHeight(10)));
		MatcherAssert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		MatcherAssert.assertThat(ai.getOutlinksSize(new BlockHeight(10)), IsEqual.equalTo(2));
	}

	// endregion

	// region {get|set}Importance

	@Test
	public void getImportanceReturnsZeroWhenUnset() {
		// Arrange:
		final ReadOnlyAccountImportance ai = new AccountImportance();

		// Act:
		MatcherAssert.assertThat(ai.getImportance(new BlockHeight(7)), IsEqual.equalTo(0.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotGetImportanceWhenItIsNotSetAtHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		ai.getImportance(new BlockHeight(7));
	}

	@Test
	public void canGetImportanceWhenItIsSetAtHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		final double importance = ai.getImportance(new BlockHeight(5));

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(importance, IsEqual.equalTo(17.0));
		MatcherAssert.assertThat(ai.getHeight(), IsEqual.equalTo(new BlockHeight(5)));
	}

	@Test
	public void importanceSetAtHeightCanBeUpdated() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		ai.setImportance(new BlockHeight(5), 11);
		final double importance = ai.getImportance(new BlockHeight(5));

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(importance, IsEqual.equalTo(11.0));
		MatcherAssert.assertThat(ai.getHeight(), IsEqual.equalTo(new BlockHeight(5)));
	}

	@Test
	public void canSetImportanceAtNewHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		ai.setImportance(new BlockHeight(6), 12);
		final double importance = ai.getImportance(new BlockHeight(6));

		// Assert:
		MatcherAssert.assertThat(ai.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(importance, IsEqual.equalTo(12.0));
		MatcherAssert.assertThat(ai.getHeight(), IsEqual.equalTo(new BlockHeight(6)));
	}

	// endregion

	// region {get|set}LastPageRank

	@Test
	public void pageRankIsInitiallyZero() {
		// Arrange:
		final ReadOnlyAccountImportance ai = new AccountImportance();

		// Assert:
		MatcherAssert.assertThat(ai.getLastPageRank(), IsEqual.equalTo(0.0));
	}

	@Test
	public void pageRankCanBeSet() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.setLastPageRank(12);

		// Assert:
		MatcherAssert.assertThat(ai.getLastPageRank(), IsEqual.equalTo(12.0));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesLatestImportanceInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);
		ai.setLastPageRank(12.0);

		// Act:
		final ReadOnlyAccountImportance copy = ai.copy();
		final double importance = copy.getImportance(new BlockHeight(5));

		// Assert:
		MatcherAssert.assertThat(copy.isSet(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(importance, IsEqual.equalTo(17.0));
		MatcherAssert.assertThat(copy.getHeight(), IsEqual.equalTo(new BlockHeight(5)));
		MatcherAssert.assertThat(copy.getLastPageRank(), IsEqual.equalTo(12.0));
	}

	@Test
	public void copyCopiesHistoricalOutlinkInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));

		// Act:
		final ReadOnlyAccountImportance copy = ai.copy();

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(NisUtils.createLink(7, 27, "BBB"), NisUtils.createLink(8, 35, "CCC"),
				NisUtils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = this.toList(getOutlinksIterator(copy, new BlockHeight(9)));
		MatcherAssert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		MatcherAssert.assertThat(copy.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(3));
	}

	@Test
	public void copyCreatesDeepCopyOfHistoricalOutlinkInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.addOutlink(NisUtils.createLink(7, 27, "BBB"));
		ai.addOutlink(NisUtils.createLink(8, 35, "CCC"));
		ai.addOutlink(NisUtils.createLink(9, 18, "AAA"));

		// Act:
		final AccountImportance copy = ai.copy();
		copy.addOutlink(NisUtils.createLink(11, 14, "DDD"));

		// Assert:
		MatcherAssert.assertThat(ai.getOutlinksSize(new BlockHeight(15)), IsEqual.equalTo(3));
		MatcherAssert.assertThat(copy.getOutlinksSize(new BlockHeight(15)), IsEqual.equalTo(4));
	}

	// endregion

	// region toString

	@Test
	public void toStringCreatesAppropriateStringRepresentation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Assert:
		MatcherAssert.assertThat(ai.toString(), IsEqual.equalTo("(5 : 17.000000)"));
	}

	// endregion

	private List<AccountLink> toList(final Iterator<AccountLink> linkIterator) {
		final List<AccountLink> links = new ArrayList<>();
		while (linkIterator.hasNext()) {
			links.add(linkIterator.next());
		}

		return links;
	}

	private static Iterator<AccountLink> getOutlinksIterator(final ReadOnlyAccountImportance ai, final BlockHeight endHeight) {
		final BlockHeight startHeight = new BlockHeight(Math.max(1, endHeight.getRaw() - OUTLINK_HISTORY));
		return ai.getOutlinksIterator(startHeight, endHeight);
	}
}
