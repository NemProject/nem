package org.nem.nis.dao.retrievers;

import java.util.*;
import org.hibernate.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

/**
 * Class for for retrieving mosaic supplies.
 */
public class MosaicSupplyRetriever {
	final private int namespaceLifetime;

	/**
	 * Creates a retriever with default settings.
	 */
	public MosaicSupplyRetriever() {
		this(NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay() * (365 + 30 + 1));
	}

	/**
	 * Creates a retriever with custom namespace lifetime.
	 *
	 * @param namespaceLifetime Number of blocks a namespace is active before being pruned (includes grace period).
	 */
	public MosaicSupplyRetriever(final int namespaceLifetime) {
		this.namespaceLifetime = namespaceLifetime;
	}

	/**
	 * Gets a mosaic definition and its corresponding supply at a specified height.
	 *
	 * @param session The session.
	 * @param mosaicId The mosaic id.
	 * @param height The search height.
	 * @return The db mosaic definition and supply tuple, if found. \c null otherwise.
	 */
	public DbMosaicDefinitionSupplyTuple getMosaicDefinitionWithSupply(final Session session, final MosaicId mosaicId, final Long height) {
		final List<DbMosaicDefinitionCreationTransaction> creationTransactions = MosaicSupplyRetriever.queryCreationTransactions(session,
				mosaicId, height);

		Long supply = 0L;
		Long firstCreationHeight = 0L;
		Long lastCreationHeight = 0L;
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
				lastCreationHeight = transaction.getBlock().getHeight();
			}

			firstCreationHeight = transaction.getBlock().getHeight();
			dbMosaicDefinitionIds.add(mosaicDefinition.getId());
		}

		if (0L == firstCreationHeight)
			return null;

		final List<DbMosaicSupplyChangeTransaction> supplyChangeTransactions = MosaicSupplyRetriever.querySupplyChangeTransactions(session,
				dbMosaicDefinitionIds, firstCreationHeight, height);

		for (final DbMosaicSupplyChangeTransaction transaction : supplyChangeTransactions) {
			if (MosaicSupplyType.Create.value() == transaction.getSupplyType()) {
				supply += transaction.getQuantity();
			} else {
				supply -= transaction.getQuantity();
			}
		}

		final Long expirationHeight = this.findExpirationHeight(session, matchingMosaicDefinition.getNamespaceId(), lastCreationHeight);

		return new DbMosaicDefinitionSupplyTuple(matchingMosaicDefinition, new Supply(supply), new BlockHeight(expirationHeight));
	}

	private static List<DbMosaicDefinitionCreationTransaction> queryCreationTransactions(final Session session, final MosaicId mosaicId,
			final Long height) {
		// JOIN FETCH everything the former Criteria query joined implicitly (the lazy block and every eager to-one); HQL loads each
		// eager association left out here with a separate select per row (N+1)
		final String hql = "SELECT t FROM DbMosaicDefinitionCreationTransaction t" // preserve-newline
				+ " JOIN FETCH t.block b LEFT JOIN FETCH b.harvester LEFT JOIN FETCH b.lessor" // preserve-newline
				+ " JOIN FETCH t.mosaicDefinition md LEFT JOIN FETCH md.creator LEFT JOIN FETCH md.feeRecipient" // preserve-newline
				+ " LEFT JOIN FETCH t.sender LEFT JOIN FETCH t.creationFeeSink" // preserve-newline
				+ " WHERE md.namespaceId = :namespaceId AND md.name = :name AND b.height <= :height" // preserve-newline
				+ " ORDER BY b.height DESC, t.blkIndex DESC";
		return session.createQuery(hql, DbMosaicDefinitionCreationTransaction.class)
				.setParameter("namespaceId", mosaicId.getNamespaceId().toString()).setParameter("name", mosaicId.getName())
				.setParameter("height", height).list();
	}

	private static List<DbMosaicSupplyChangeTransaction> querySupplyChangeTransactions(final Session session,
			final Collection<Long> dbMosaicDefinitionIds, final Long startHeight, final Long endHeight) {
		// JOIN FETCH everything the former Criteria query joined implicitly (the lazy block and every eager to-one); HQL loads each
		// eager association left out here with a separate select per row (N+1)
		// callers guarantee a non-empty id collection (getMosaicDefinitionWithSupply returns null first)
		final String hql = "SELECT t FROM DbMosaicSupplyChangeTransaction t" // preserve-newline
				+ " JOIN FETCH t.block b LEFT JOIN FETCH b.harvester LEFT JOIN FETCH b.lessor LEFT JOIN FETCH t.sender" // preserve-newline
				+ " WHERE t.dbMosaicId IN (:ids) AND b.height >= :startHeight AND b.height <= :endHeight" // preserve-newline
				+ " ORDER BY b.height DESC";
		return session.createQuery(hql, DbMosaicSupplyChangeTransaction.class).setParameterList("ids", dbMosaicDefinitionIds)
				.setParameter("startHeight", startHeight).setParameter("endHeight", endHeight).list();
	}

	private static DbNamespace queryLastRootNamespace(final Session session, final String namespaceId, final Long maxHeight) {
		final NamespaceId rootNamespaceId = new NamespaceId(namespaceId).getRoot();
		// n.owner is eager; without the JOIN FETCH, HQL would load it with a separate select per row (N+1)
		final List<DbNamespace> namespaces = session
				.createQuery("SELECT n FROM DbNamespace n LEFT JOIN FETCH n.owner"
						+ " WHERE n.fullName = :fullName AND n.height < :maxHeight ORDER BY n.height DESC", DbNamespace.class)
				.setParameter("fullName", rootNamespaceId.toString()).setParameter("maxHeight", maxHeight).setMaxResults(1).list();
		return namespaces.get(0);
	}

	private static List<DbNamespace> querySubsequentRootNamespaces(final Session session, final String namespaceId, final Long minHeight) {
		final NamespaceId rootNamespaceId = new NamespaceId(namespaceId).getRoot();
		// n.owner is eager; without the JOIN FETCH, HQL would load it with a separate select per row (N+1)
		return session
				.createQuery("SELECT n FROM DbNamespace n LEFT JOIN FETCH n.owner"
						+ " WHERE n.fullName = :fullName AND n.height > :minHeight ORDER BY n.height ASC", DbNamespace.class)
				.setParameter("fullName", rootNamespaceId.toString()).setParameter("minHeight", minHeight).list();
	}

	private Long findExpirationHeight(final Session session, final String namespaceId, final Long height) {
		final DbNamespace lastRootNamespace = MosaicSupplyRetriever.queryLastRootNamespace(session, namespaceId, height);
		final List<DbNamespace> subsequentRootNamespaces = MosaicSupplyRetriever.querySubsequentRootNamespaces(session, namespaceId,
				height);

		DbNamespace activeRootNamespace = lastRootNamespace;
		for (final DbNamespace subsequentRootNamespace : subsequentRootNamespaces) {
			if (subsequentRootNamespace.getHeight() > activeRootNamespace.getHeight() + this.namespaceLifetime) {
				// subsequentRootNamespace was registered after grace period expiration and pruning
				// so it should be treated as distinct
				break;
			}

			activeRootNamespace = subsequentRootNamespace;
		}

		return activeRootNamespace.getHeight() + this.namespaceLifetime;
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
		return lhs.getCreator().getId() == rhs.getCreator().getId() // preserve-newline
				&& lhs.getFeeType() == rhs.getFeeType() // preserve-newline
				&& lhs.getFeeDbMosaicId() == rhs.getFeeDbMosaicId() // preserve-newline
				&& lhs.getFeeQuantity() == rhs.getFeeQuantity() // preserve-newline
				&& (null == lhs.getFeeRecipient()) == (null == rhs.getFeeRecipient())
				&& (null == lhs.getFeeRecipient() || lhs.getFeeRecipient().getId() == rhs.getFeeRecipient().getId());
	}
}
