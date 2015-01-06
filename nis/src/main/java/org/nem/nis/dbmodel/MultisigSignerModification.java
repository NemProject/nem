package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.Set;

/**
 * Multisig Signer Modification db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.MULTISIG_SIGNER_MODIFY
 * <p>
 *
 * TODO 20150103 J-G can you remind me why we need both MultisigSignerModification and MultisigModification?
 * > is it so that we can make multiple modifications in one transaction;
 * > if so, i think the naming is a little misleading
 * TODO 20150105 G-J that is why, any proposals for names?
 * MultisigModification / MultisigModifications
 * MultisigModification / AggregateMultisigModification
 *
 * TODO 20150105 G-J slightly off-topic, I was thinking about naming all the dbmodel classes exactly like
 * > model classes, but with "Db" prefix, prefix to make code that uses both models and dbmodels (i.e. mappers)
 * > nicer to look at. would you be ok with that?
 * TODO 20150105 J-G: good suggestion
 */
@Entity
@Table(name = "multisigsignermodifications")
public class MultisigSignerModification extends AbstractBlockTransfer<MultisigSignerModification> {
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigSignerModification", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<DbMultisigModification> multisigModifications;

	public MultisigSignerModification() {
		super(b -> b.getBlockMultisigSignerModifications());
	}

	public Set<DbMultisigModification> getMultisigModifications() {
		return this.multisigModifications;
	}

	public void setMultisigModifications(final Set<DbMultisigModification> multisigModifications) {
		this.multisigModifications = multisigModifications;
	}
}
