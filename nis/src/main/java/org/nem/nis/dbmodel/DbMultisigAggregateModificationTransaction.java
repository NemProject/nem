package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Multisig Signer Modification db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION
 */
@Entity
@Table(name = "multisigsignermodifications")
public class DbMultisigAggregateModificationTransaction extends AbstractBlockTransfer<DbMultisigAggregateModificationTransaction> {
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigAggregateModificationTransaction", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<DbMultisigModification> multisigModifications;

	public DbMultisigAggregateModificationTransaction() {
		super(b -> b.getBlockMultisigAggregateModificationTransactions());
	}

	public Set<DbMultisigModification> getMultisigModifications() {
		return this.multisigModifications;
	}

	public void setMultisigModifications(final Set<DbMultisigModification> multisigModifications) {
		this.multisigModifications = multisigModifications;
	}

	public Collection<DbAccount> getOtherAccounts() {
		return this.multisigModifications.stream().map(DbMultisigModification::getCosignatory).collect(Collectors.toList());
	}
}
