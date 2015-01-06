package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * A mapping that is able to map a db block to a model block.
 */
public class BlockDbModelToModelMapping implements IMapping<DbBlock, Block> {
	private final IMapper mapper;
	private final AccountLookup accountLookup;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 * @param accountLookup The account lookup.
	 */
	public BlockDbModelToModelMapping(
			final IMapper mapper,
			final AccountLookup accountLookup) {
		this.mapper = mapper;
		this.accountLookup = accountLookup;
	}

	@Override
	public Block map(final DbBlock dbBlock) {
		if (1 == dbBlock.getHeight()) {
			// TODO 20141226 J-G: do you remember why we have this special case / do we still need it after separating the account and account state?
			return NemesisBlock.fromResource(new DeserializationContext(this.accountLookup));
		}

		final Account forager = this.mapper.map(dbBlock.getHarvester(), Account.class);
		final Account lessor = dbBlock.getLessor() != null ? this.mapper.map(dbBlock.getLessor(), Account.class) : null;

		final Block block = new org.nem.core.model.Block(
				forager,
				dbBlock.getPrevBlockHash(),
				dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()),
				new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getHarvesterProof()));

		final int count = StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
				.map(e -> e.getFromBlock.apply(dbBlock).size())
				.reduce(0, Integer::sum);

		// TODO: there is a bug here, "inner" transactions should not be counted in,
		// when having MultisigTransaction with some inner transaction, count should be 1, but it is 2
		final ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction[count]));
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			for (final AbstractBlockTransfer dbTransfer : entry.getFromBlock.apply(dbBlock)) {
				final Transaction transaction = this.mapper.map(dbTransfer, Transaction.class);
				transactions.set(dbTransfer.getBlkIndex(), transaction);
			}
		}

		block.addTransactions(transactions);
		return block;
	}
}
