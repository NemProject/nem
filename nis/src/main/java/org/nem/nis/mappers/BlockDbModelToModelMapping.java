package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.*;

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
		// TODO 20141226 J-G: do you remember why we have this special case / do we still need it after separating the account and account state?
		// TODO 20150309 BR -> J: works perfectly without this block of code.
		// TODO 20150309 J -> B: after removing this line there is an issue syncing from scratch (i didn't have time to debug)
		// TODO 20150310 BR -> J: it is the block type/block class, I should have tested syncing from scratch :/ There is an easy fix but now
		// > the nemesis block is not mapped to a NemesisBlock object which makes BlockDbModelToModelMappingTest.nemesisDbModelCanBeMappedToNemesisModel() fail.
		// > Is it really important that the nemesis block is mapped to its own class?
		// > Anyway, i created a new branch in order not to pollute the todo20150301 branch.

		final Account harvester = this.mapper.map(dbBlock.getHarvester(), Account.class);
		final Account lessor = dbBlock.getLessor() != null ? this.mapper.map(dbBlock.getLessor(), Account.class) : null;

		final Block block = new org.nem.core.model.Block(
				harvester,
				dbBlock.getPrevBlockHash(),
				dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()),
				new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getHarvesterProof()));

		final int count = (int)getAllDirectBlockTransfers(dbBlock).count();
		final ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction[count]));
		getAllDirectBlockTransfers(dbBlock).forEach(dbTransfer -> {
			final Transaction transaction = this.mapper.map(dbTransfer, Transaction.class);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		});

		block.addTransactions(transactions);
		return block;
	}

	private static Stream<AbstractBlockTransfer> getAllDirectBlockTransfers(final DbBlock dbBlock) {
		return StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
				.flatMap(e -> e.getFromBlock.apply(dbBlock).stream())
				.filter(t -> !DbModelUtils.isInnerTransaction(t));
	}
}
