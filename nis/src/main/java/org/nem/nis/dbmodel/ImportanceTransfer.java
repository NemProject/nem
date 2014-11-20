package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Importance transfer db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.IMPORTANCE_TYPE
 * <p>
 */
@Entity
@Table(name = "importancetransfers")
public class ImportanceTransfer extends AbstractTransfer<ImportanceTransfer> {
	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "remoteId")
	private Account remote;

	private Integer mode;

	public ImportanceTransfer() {
		super(b -> b.getBlockImportanceTransfers());
	}

	public Account getRemote() {
		return this.remote;
	}

	public void setRemote(final Account remote) {
		this.remote = remote;
	}

	public Integer getMode() {
		return this.mode;
	}

	public void setMode(final Integer mode) {
		this.mode = mode;
	}
}
