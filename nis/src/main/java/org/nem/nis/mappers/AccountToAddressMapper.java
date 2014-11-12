package org.nem.nis.mappers;

import org.nem.core.model.Address;
import org.nem.nis.dbmodel.Account;

public class AccountToAddressMapper {
	public static Address toAddress(final Account account) {
		return new Address(account.getPublicKey(), account.getPrintableKey());
	}
}
