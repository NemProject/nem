package org.nem.nis.mappers;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.nis.dbmodel.Account;

/**
 * Static class that contains functions for converting to and from
 * db-model Account and model Address.
 */
public class AccountToAddressMapper {

	/**
	 * Converts an Account dbmodel to an Address model.
	 *
	 * @param account The dbmodel account.
	 * @return The model address
	 */
	public static Address toAddress(final Account account) {
		// AddressWithoutEncodedAddressGeneration is used as an optimization to avoid
		// regenerating an encoded address since we already have it in our database
		return new AddressWithoutEncodedAddressGeneration(account.getPublicKey(), account.getPrintableKey());
	}

	private static class AddressWithoutEncodedAddressGeneration extends Address {
		public AddressWithoutEncodedAddressGeneration(final PublicKey publicKey, final String encoded) {
			super(publicKey, encoded);
		}
	}
}
