package org.nem.core.model;

/**
 * Enum containing types of multisig modifications.
 */
public enum MultisigModificationType {
	/**
	 * An unknown mode.
	 */
	Unknown(0),

	/**
	 * When adding cosignatory to multisig account.
	 */
	Add(1);

	/**
	 * For now we WON'T allow removal...
	 * TODO 20141112 J-G: do you have a concern in mind or are just dropping it for expediency?
	 * TODO 20131113 G-J,B: actually it's not about implementation part, but about real life part.
	 *  1. Let's say I create multisig acct for some company and add 3 addresses mine X, and A, B, C
	 *  2. Company adds fund to the account
	 *  3. Now I remove A, B and C, and withdraw funds from an account and go to Bahamas...
	 *
	 *  I'm not sure how removal of account should be done...
	 *
	 * TODO 20131113 G-J,B: my idea (may be flawed), was as follows:
	 * 22:57 <@gimre> 1. making "add cosignatory" transaction (i.e. add X as cosigner of M), changes account M to multisig
	 * 22:58 <@gimre> and than you simply add next accounts
	 * 22:58 <@gimre> and than transfer from M to anywhere
	 * 22:59 <@gimre> requires signatures from all cosignatories
	 * 22:59 <@gimre> also, there would probably have to be "wait" time, before cosignatory becomes valid...
	 * 23:00 <@gimre> (similar to that with activation of remote harvesting account)
	 * 23:02 <@BloodyRookie> is it really needed to change a multisig account once it is created? (like adding new cosigners)
	 * 23:03 <@gimre> BloodyRookie: most likely not, but what I wanted was to have "add cosigner" as a separate transactions,
	 * as that most likely will be easier to handle
	 * 'is it really needed to change a multisig account once it is created' - RH had a scenario ... there is an account for a company
	 * where each member of the board has a private key ... when the board changes, the people in charge of the funds need to change
	 * concretely, for the NEM post-launch account, if someone leaves NEM their key should be revoked (and probably replaced with someone else)
	 * removal addition could require full consensus
	 */
	// Del(2)

	private final int value;

	MultisigModificationType(final int value) {
		this.value = value;
	}

	public boolean isValid() {
		switch (this) {
			case Add:
				return true;
		}

		return false;
	}

	/**
	 * Creates a mode given a raw value.
	 *
	 * @param value The value.
	 * @return The mode if the value is known or Unknown if it was not.
	 */
	public static MultisigModificationType fromValueOrDefault(final int value) {
		for (final MultisigModificationType modificationType : values()) {
			if (modificationType.value() == value) {
				return modificationType;
			}
		}

		return MultisigModificationType.Unknown;
	}

	/**
	 * Gets the underlying integer representation of the mode.
	 *
	 * @return The underlying value.
	 */
	public int value() {
		return this.value;
	}
}
