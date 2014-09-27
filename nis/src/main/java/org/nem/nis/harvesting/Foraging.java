package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.validators.TransactionValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TODO 20140926 J-J: this class doesn't really do anything and can / should be removed

//
// Initial logic is as follows:
//   * we receive new TX, IF it hasn't been seen,
//     it is added to unconfirmedTransactions,
//   * blockGeneratorExecutor periodically tries to generate a block containing
//     unconfirmed transactions
//   * if it succeeded, block is added to the db and propagated to the network
//
// fork resolution should solve the rest
//
public class Foraging {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	private static final int TRANSACTION_MAX_ALLOWED_TIME_DEVIATION = 30;

	private final Harvester harvester;
	private final UnconfirmedTransactions unconfirmedTransactions;

	@Autowired(required = true)
	public Foraging(
			final Harvester harvester,
			final UnconfirmedTransactions unconfirmedTransactions) {
		this.harvester = harvester;
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	/**
	 * Checks if transaction fits in time limit window, if so add it to
	 * list of unconfirmed transactions.
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return NEUTRAL if given transaction has already been seen or isn't within the time window, SUCCESS if it has been added
	 */
	public ValidationResult processTransaction(final Transaction transaction) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(TRANSACTION_MAX_ALLOWED_TIME_DEVIATION)) > 0) {
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE;
		}
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(-TRANSACTION_MAX_ALLOWED_TIME_DEVIATION)) < 0) {
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_PAST;
		}

		return this.unconfirmedTransactions.add(transaction);
	}

	/**
	 * Processes every transaction in the list.
	 * Since this method is called in the synchronization process, it doesn't make sense to return a value.
	 *
	 * @param transactions The transactions.
	 */
	public void processTransactions(final Collection<Transaction> transactions) {
		transactions.stream().forEach(tx -> this.processTransaction(tx));
	}
}
