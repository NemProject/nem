package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Importance transfer db entity <br>
 * Holds information about Transactions having type TransactionTypes.IMPORTANCE_TYPE
 */
@Entity
@Table(name = "importancetransfers")
public class DbImportanceTransferTransaction extends AbstractBlockTransfer<DbImportanceTransferTransaction> {
	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "remoteId")
	private DbAccount remote;

	private Integer mode;

	public DbAccount getRemote() {
		return this.remote;
	}

	public void setRemote(final DbAccount remote) {
		this.remote = remote;
	}

	public Integer getMode() {
		return this.mode;
	}

	public void setMode(final Integer mode) {
		this.mode = mode;
	}
}
