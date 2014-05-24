package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class AccountImportanceTest {

	//region out-links

	@Test
	public void canAddOutlinks() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(Utils.createLink(7, 27, "BBB"));
		ai.addOutlink(Utils.createLink(8, 35, "CCC"));
		ai.addOutlink(Utils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(
				Utils.createLink(7, 27, "BBB"),
				Utils.createLink(8, 35, "CCC"),
				Utils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = toList(ai.getOutlinksIterator(new BlockHeight(9)));
		Assert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		Assert.assertThat(ai.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(3));
	}

	@Test
	public void canRemoveOutlinks() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(Utils.createLink(7, 27, "BBB"));
		ai.addOutlink(Utils.createLink(8, 35, "CCC"));
		ai.removeOutlink(Utils.createLink(8, 35, "CCC"));
		ai.addOutlink(Utils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(
				Utils.createLink(7, 27, "BBB"),
				Utils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = toList(ai.getOutlinksIterator(new BlockHeight(9)));
		Assert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		Assert.assertThat(ai.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(2));
	}

	@Test
	public void outlinkGettersRespectBlockHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.addOutlink(Utils.createLink(7, 27, "BBB"));
		ai.addOutlink(Utils.createLink(8, 35, "CCC"));
		ai.addOutlink(Utils.createLink(9, 18, "AAA"));

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(
				Utils.createLink(7, 27, "BBB"),
				Utils.createLink(8, 35, "CCC"));

		final List<AccountLink> links = toList(ai.getOutlinksIterator(new BlockHeight(8)));
		Assert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		Assert.assertThat(ai.getOutlinksSize(new BlockHeight(8)), IsEqual.equalTo(2));
	}

	//endregion

	//region {get|set}Importance

	@Test(expected = IllegalArgumentException.class)
	public void cannotGetImportanceWhenItIsUnset() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();

		// Act:
		ai.getImportance(new BlockHeight(7));
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
		Assert.assertThat(importance, IsEqual.equalTo(17.0));
	}

	@Test
	public void canUpdateImportanceWhenAtHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		ai.setImportance(new BlockHeight(5), 11);
		final double importance = ai.getImportance(new BlockHeight(5));

		// Assert:
		Assert.assertThat(importance, IsEqual.equalTo(11.0));
	}

	// TODO: this fails because setImportance has a bug
	@Test
	public void cannotSetImportanceAtNewHeight() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

		// Act:
		ai.setImportance(new BlockHeight(6), 12);
		final double importance = ai.getImportance(new BlockHeight(6));

		// Assert:
		Assert.assertThat(importance, IsEqual.equalTo(12.0));
	}

	//endregion

	//region copy

	@Test
	public void copyCopiesLatestImportanceInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.setImportance(new BlockHeight(5), 17);

			// Act:
		final AccountImportance copy = ai.copy();
		final double importance = copy.getImportance(new BlockHeight(5));

		// Assert:
		Assert.assertThat(importance, IsEqual.equalTo(17.0));
	}

	@Test
	public void copyCopiesHistoricalOutlinkInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.addOutlink(Utils.createLink(7, 27, "BBB"));
		ai.addOutlink(Utils.createLink(8, 35, "CCC"));
		ai.addOutlink(Utils.createLink(9, 18, "AAA"));

		// Act:
		final AccountImportance copy = ai.copy();

		// Assert:
		final List<AccountLink> expectedLinks = Arrays.asList(
				Utils.createLink(7, 27, "BBB"),
				Utils.createLink(8, 35, "CCC"),
				Utils.createLink(9, 18, "AAA"));

		final List<AccountLink> links = toList(copy.getOutlinksIterator(new BlockHeight(9)));
		Assert.assertThat(links, IsEquivalent.equivalentTo(expectedLinks));
		Assert.assertThat(copy.getOutlinksSize(new BlockHeight(9)), IsEqual.equalTo(3));
	}

	@Test
	public void copyCreatesDeepCopyOfHistoricalOutlinkInformation() {
		// Arrange:
		final AccountImportance ai = new AccountImportance();
		ai.addOutlink(Utils.createLink(7, 27, "BBB"));
		ai.addOutlink(Utils.createLink(8, 35, "CCC"));
		ai.addOutlink(Utils.createLink(9, 18, "AAA"));

		// Act:
		final AccountImportance copy = ai.copy();
		copy.addOutlink(Utils.createLink(11, 14, "DDD"));

		// Assert:
		Assert.assertThat(ai.getOutlinksSize(new BlockHeight(15)), IsEqual.equalTo(3));
		Assert.assertThat(copy.getOutlinksSize(new BlockHeight(15)), IsEqual.equalTo(4));
	}

	//endregion

	private List<AccountLink> toList(final Iterator<AccountLink> linkIterator) {
		final List<AccountLink> links = new ArrayList<>();
		while (linkIterator.hasNext())
			links.add(linkIterator.next());

		return links;
	}
}