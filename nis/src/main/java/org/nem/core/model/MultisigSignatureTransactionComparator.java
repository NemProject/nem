package org.nem.core.model;

import org.nem.core.utils.ArrayUtils;

import java.util.Comparator;

/**
 * A custom comparator for comparing MultisigSignatureTransaction objects.
 * <br>
 * This comparator only looks at the transaction signer and other hash.
 */
public class MultisigSignatureTransactionComparator implements Comparator<MultisigSignatureTransaction> {

	@Override
	public int compare(final MultisigSignatureTransaction lhs, final MultisigSignatureTransaction rhs) {
		final Address lhsAddress = lhs.getSigner().getAddress();
		final Address rhsAddress = rhs.getSigner().getAddress();
		final int addressCompareResult = lhsAddress.compareTo(rhsAddress);
		if (addressCompareResult != 0) {
			return addressCompareResult;
		}

		return ArrayUtils.compare(lhs.getOtherTransactionHash().getRaw(), rhs.getOtherTransactionHash().getRaw());
	}
}
