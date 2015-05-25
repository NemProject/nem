package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class MultisigModificationTypeTest {

	//region isValid

	@Test
	public void isValidReturnsTrueForValidModifications() {
		// Assert:
		Assert.assertThat(MultisigModificationType.Add_Cosignatory.isValid(), IsEqual.equalTo(true));
		Assert.assertThat(MultisigModificationType.Del_Cosignatory.isValid(), IsEqual.equalTo(true));
		Assert.assertThat(MultisigModificationType.Min_Cosignatories.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void isValidReturnsFalseForInvalidModifications() {
		Assert.assertThat(MultisigModificationType.Unknown.isValid(), IsEqual.equalTo(false));
	}

	//endregion

	//region fromValueOrDefault

	@Test
	public void fromValueOrDefaultReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		Assert.assertThat(MultisigModificationType.fromValueOrDefault(1), IsEqual.equalTo(MultisigModificationType.Add_Cosignatory));
		Assert.assertThat(MultisigModificationType.fromValueOrDefault(2), IsEqual.equalTo(MultisigModificationType.Del_Cosignatory));
		Assert.assertThat(MultisigModificationType.fromValueOrDefault(3), IsEqual.equalTo(MultisigModificationType.Min_Cosignatories));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		Assert.assertThat(MultisigModificationType.fromValueOrDefault(0), IsEqual.equalTo(MultisigModificationType.Unknown));
		Assert.assertThat(MultisigModificationType.fromValueOrDefault(9999), IsEqual.equalTo(MultisigModificationType.Unknown));
	}

	//endregion
}