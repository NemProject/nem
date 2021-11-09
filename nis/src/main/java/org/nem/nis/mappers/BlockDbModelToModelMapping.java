package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.*;

/**
 * A mapping that is able to map a db block to a model block.
 */
public class BlockDbModelToModelMapping implements IMapping<DbBlock, Block> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public BlockDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Block map(final DbBlock dbBlock) {
		final Account harvester = this.mapper.map(dbBlock.getHarvester(), Account.class);
		final Account lessor = dbBlock.getLessor() != null ? this.mapper.map(dbBlock.getLessor(), Account.class) : null;

		final Block block = new org.nem.core.model.Block(harvester, dbBlock.getPrevBlockHash(), dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()), new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getHarvesterProof()));

		final int count = (int) getAllDirectBlockTransfers(dbBlock).count();
		final ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction[count]));
		getAllDirectBlockTransfers(dbBlock).forEach(dbTransfer -> {
			final Transaction transaction = this.mapper.map(dbTransfer, Transaction.class);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		});

		block.addTransactions(transactions);
		return block;
	}
	@SuppressWarnings("rawtypes")
	private static Stream<AbstractBlockTransfer> getAllDirectBlockTransfers(final DbBlock dbBlock) {
		return StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false).flatMap(e -> e.getFromBlock.apply(dbBlock).stream())
				.filter(t -> !DbModelUtils.isInnerTransaction(t));
	}
}
