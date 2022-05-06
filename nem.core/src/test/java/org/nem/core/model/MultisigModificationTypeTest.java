package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class MultisigModificationTypeTest {

	// region isValid

	@Test
	public void isValidReturnsTrueForValidModifications() {
		// Assert:
		MatcherAssert.assertThat(MultisigModificationType.AddCosignatory.isValid(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(MultisigModificationType.DelCosignatory.isValid(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(MultisigModificationType.MinCosignatories.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void isValidReturnsFalseForInvalidModifications() {
		MatcherAssert.assertThat(MultisigModificationType.Unknown.isValid(), IsEqual.equalTo(false));
	}

	// endregion

	// region fromValueOrDefault

	@Test
	public void fromValueOrDefaultReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		MatcherAssert.assertThat(MultisigModificationType.fromValueOrDefault(1), IsEqual.equalTo(MultisigModificationType.AddCosignatory));
		MatcherAssert.assertThat(MultisigModificationType.fromValueOrDefault(2), IsEqual.equalTo(MultisigModificationType.DelCosignatory));
		MatcherAssert.assertThat(MultisigModificationType.fromValueOrDefault(3),
				IsEqual.equalTo(MultisigModificationType.MinCosignatories));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		MatcherAssert.assertThat(MultisigModificationType.fromValueOrDefault(0), IsEqual.equalTo(MultisigModificationType.Unknown));
		MatcherAssert.assertThat(MultisigModificationType.fromValueOrDefault(9999), IsEqual.equalTo(MultisigModificationType.Unknown));
	}

	// endregion
}
