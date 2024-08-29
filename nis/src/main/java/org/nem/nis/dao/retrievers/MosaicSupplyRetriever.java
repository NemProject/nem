package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.MosaicSupplyType;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for for retrieving mosaic supplies.
 */
public class MosaicSupplyRetriever {
	/**
	 * Gets a mosaic definition and its corresponding supply at a specified height.
	 *
	 * @param session The session.
	 * @param mosaicId The mosaic id.
	 * @param height The search height.
	 * @return The db mosaic definition and supply, if found. \c null otherwise.
	 */
	public DbMosaicDefinitionSupplyPair getMosaicDefinitionWithSupply(final Session session, final MosaicId mosaicId, final Long height) {
		final List<DbMosaicDefinitionCreationTransaction> creationTransactions = MosaicSupplyRetriever.queryCreationTransactions(session, mosaicId, height);

		Long supply = 0L;
		Long firstCreationHeight = 0L;
		DbMosaicDefinition matchingMosaicDefinition = null;
		final Collection<Long> dbMosaicDefinitionIds = new HashSet<>();
		for (final DbMosaicDefinitionCreationTransaction transaction : creationTransactions) {
			final DbMosaicDefinition mosaicDefinition = transaction.getMosaicDefinition();
			if (null != matchingMosaicDefinition) {
				if (!MosaicSupplyRetriever.areDefinitionsEqual(matchingMosaicDefinition, mosaicDefinition)) {
					break;
				}
			} else {
				final DbMosaicProperty mosaicProperty = MosaicSupplyRetriever.findPropertyByName(mosaicDefinition, "initialSupply");
				supply = Long.parseLong(mosaicProperty.getValue(), 10);
				matchingMosaicDefinition = mosaicDefinition;
			}

			firstCreationHeight = transaction.getBlock().getHeight();
			dbMosaicDefinitionIds.add(mosaicDefinition.getId());
		}

		if (0L == firstCreationHeight)
			return null;

		final List<DbMosaicSupplyChangeTransaction> supplyChangeTransactions = MosaicSupplyRetriever.querySupplyChangeTransactions(
			session, dbMosaicDefinitionIds, firstCreationHeight, height);

		for (final DbMosaicSupplyChangeTransaction transaction : supplyChangeTransactions) {
			if (MosaicSupplyType.Create.value() == transaction.getSupplyType()) {
				supply += transaction.getQuantity();
			} else {
				supply -= transaction.getQuantity();
			}
		}

		return new DbMosaicDefinitionSupplyPair(matchingMosaicDefinition, new Supply(supply));
	}

	private static List<DbMosaicDefinitionCreationTransaction> queryCreationTransactions(final Session session, final MosaicId mosaicId, final Long height) {
		final Criteria creationTransactionsCriteria = session.createCriteria(DbMosaicDefinitionCreationTransaction.class, "transaction") // preserve-newline
				.setFetchMode("sender", FetchMode.JOIN) // preserve-newline
				.createAlias("transaction.block", "block") // preserve-newline
				.createAlias("transaction.mosaicDefinition", "mosaicDefinition") // preserve-newline
				.add(Restrictions.eq("mosaicDefinition.namespaceId", mosaicId.getNamespaceId().toString())) // preserve-newline
				.add(Restrictions.eq("mosaicDefinition.name", mosaicId.getName())) // preserve-newline
				.add(Restrictions.le("block.height", height)) // preserve-newline
				.addOrder(Order.desc("block.height")) // preserve-newline
				.addOrder(Order.desc("blkIndex")) // preserve-newline
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return HibernateUtils.listAndCast(creationTransactionsCriteria);
	}

	private static List<DbMosaicSupplyChangeTransaction> querySupplyChangeTransactions(final Session session,
			final Collection<Long> dbMosaicDefinitionIds, final Long startHeight, final Long endHeight) {
		final Criteria supplyChangeTransactionCriteria = session.createCriteria(DbMosaicSupplyChangeTransaction.class, "transaction") // preserve-newline
				.setFetchMode("sender", FetchMode.JOIN) // preserve-newline
				.createAlias("transaction.block", "block") // preserve-newline
				.add(Restrictions.in("dbMosaicId", dbMosaicDefinitionIds)) // preserve-newline
				.add(Restrictions.ge("block.height", startHeight)) // preserve-newline
				.add(Restrictions.le("block.height", endHeight)) // preserve-newline
				.addOrder(Order.desc("block.height")) // preserve-newline
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return HibernateUtils.listAndCast(supplyChangeTransactionCriteria);
	}

	private static DbMosaicProperty findPropertyByName(final DbMosaicDefinition mosaicDefinition, final String name) {
		return mosaicDefinition.getProperties().stream().filter(property -> property.getName().equals(name)).findFirst().get();
	}

	private static Boolean areDefinitionsEqual(final DbMosaicDefinition lhs, final DbMosaicDefinition rhs) {
		// check mosaic properties
		for (final DbMosaicProperty property : lhs.getProperties()) {
			if (!property.getValue().equals(MosaicSupplyRetriever.findPropertyByName(rhs, property.getName()).getValue())) {
				return false;
			}
		}

		// check other properties
		return
			lhs.getCreator().getId() == rhs.getCreator().getId()
			&& lhs.getFeeType() == rhs.getFeeType()
			&& lhs.getFeeDbMosaicId() == rhs.getFeeDbMosaicId()
			&& lhs.getFeeQuantity() == rhs.getFeeQuantity()
			&& (null == lhs.getFeeRecipient()) == (null == rhs.getFeeRecipient())
			&& (null == lhs.getFeeRecipient() || lhs.getFeeRecipient().getId() == rhs.getFeeRecipient().getId());
	}
}
