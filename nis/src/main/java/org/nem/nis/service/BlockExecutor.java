package org.nem.nis.service;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for executing blocks.
 */
@Service
public class BlockExecutor {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new block executor.
	 *
	 * @param poiFacade The poi facade.
	 */
	@Autowired(required = true)
	public BlockExecutor(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	//region execute

	/**
	 * Executes all transactions in the block.
	 *
	 * @param block The block.
	 */
	public void execute(final Block block) {
		this.execute(block, new ArrayList<>());
	}

	/**
	 * Executes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void execute(final Block block, final BlockTransferObserver observer) {
		this.execute(block, Arrays.asList(observer));
	}

	/**
	 * Executes all transactions in the block with custom observers.
	 *
	 * @param block The block.
	 * @param observers The observers.
	 */
	public void execute(final Block block, final Collection<BlockTransferObserver> observers) {
		final TransferObserver observer = this.createTransferObserver(block, true, observers);

		for (final Transaction transaction : block.getTransactions()) {
			transaction.execute();
			transaction.execute(observer);
		}

		final Account signer = block.getSigner();
		signer.incrementForagedBlocks();
		signer.incrementBalance(block.getTotalFee());
		observer.notifyCredit(block.getSigner(), block.getTotalFee());
	}

	//endregion

	//region undo

	/**
	 * Undoes all transactions in the block.
	 *
	 * @param block The block.
	 */
	public void undo(final Block block) {
		this.undo(block, new ArrayList<>());
	}

	/**
	 * Undoes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void undo(final Block block, final BlockTransferObserver observer) {
		this.undo(block, Arrays.asList(observer));
	}

	/**
	 * Undoes all transactions in the block with custom observers.
	 *
	 * @param block The block.
	 * @param observers The observers.
	 */
	public void undo(final Block block, final Collection<BlockTransferObserver> observers) {
		final TransferObserver observer = this.createTransferObserver(block, false, observers);

		final Account signer = block.getSigner();
		observer.notifyDebit(block.getSigner(), block.getTotalFee());
		signer.decrementForagedBlocks();
		signer.decrementBalance(block.getTotalFee());

		for (final Transaction transaction : getReverseTransactions(block)) {
			transaction.undo(observer);
			transaction.undo();
		}
	}

	//endregion undo

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	private TransferObserver createTransferObserver(
			final Block block,
			final boolean isExecute,
			final Collection<BlockTransferObserver> observers) {
		final List<BlockTransferObserver> blockTransferObservers = new ArrayList<>();
		blockTransferObservers.add(new WeightedBalancesObserver(this.poiFacade));
		blockTransferObservers.addAll(observers);

		final TransferObserver aggregateObserver = new AggregateBlockTransferObserverToTransferObserverAdapter(
				blockTransferObservers,
				block.getHeight(),
				isExecute);
		final TransferObserver outlinkObserver = new OutlinkObserver(this.poiFacade, block.getHeight(), isExecute);

		// in an undo operation, the OutlinkObserver should be run before the balance is updated
		// (so that the matching link can be found and removed)
		final List<TransferObserver> transferObservers = Arrays.asList(aggregateObserver, outlinkObserver);
		if (!isExecute) {
			Collections.reverse(transferObservers);
		}

		return new AggregateTransferObserver(transferObservers);
	}
}
