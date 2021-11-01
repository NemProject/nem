package org.nem.nis.validators.transaction;

import java.util.logging.Logger;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that the transaction fee is at least as large as the
 * minimum fee
 */
public class MinimumFeeValidator implements SingleTransactionValidator {
	private static final Logger LOGGER = Logger.getLogger(MinimumFeeValidator.class.getName());

	private final NetworkInfo networkInfo;
	private final ReadOnlyNamespaceCache namespaceCache;
	private final boolean ignoreFees;

	/**
	 * Creates a validator.
	 *
	 * @param networkInfo The network info.
	 * @param namespaceCache The namespace cache.
	 * @param ignoreFees Flag indicating if transaction fees should be ignored.
	 */
	public MinimumFeeValidator(final NetworkInfo networkInfo, final ReadOnlyNamespaceCache namespaceCache, final boolean ignoreFees) {
		this.networkInfo = networkInfo;
		this.namespaceCache = namespaceCache;
		this.ignoreFees = ignoreFees;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (this.ignoreFees) {
			return ValidationResult.SUCCESS;
		}

		if (this.networkInfo.getNemesisBlockInfo().getAddress().equals(transaction.getSigner().getAddress())) {
			LOGGER.info(String.format("Skipping fees for transaction (%s) originating from nemesis signer account %s",
					HashUtils.calculateHash(transaction), transaction.getSigner()));
			return ValidationResult.SUCCESS;
		}

		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(this.namespaceCache);
		final TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator(adapters.asMosaicFeeInformationLookup(),
				context::getBlockHeight, new BlockHeight[]{
						new BlockHeight(BlockMarkerConstants.FEE_FORK(transaction.getVersion())),
						new BlockHeight(BlockMarkerConstants.SECOND_FEE_FORK(transaction.getVersion()))
				});
		return calculator.isFeeValid(transaction, context.getBlockHeight())
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_INSUFFICIENT_FEE;
	}
}
