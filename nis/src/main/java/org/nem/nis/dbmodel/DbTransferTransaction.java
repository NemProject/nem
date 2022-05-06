package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.*;

/**
 * Transfer Db entity. <br>
 * Holds information about Transactions having type TransactionTypes.TRANSFER_TYPE <br>
 * Associated sender and recipient are obtained automatically (by TransferDao) thanks to @Cascade annotations.
 */
@Entity
@Table(name = "transfers")
public class DbTransferTransaction extends AbstractBlockTransfer<DbTransferTransaction> {
	@ManyToOne
	@Cascade(CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "recipientId")
	private DbAccount recipient;

	private Long amount;

	private Integer messageType;
	private byte[] messagePayload;

	@OneToMany(cascade = javax.persistence.CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "transferTransaction", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Collection<DbMosaic> mosaics = new ArrayList<>();

	public DbAccount getRecipient() {
		return this.recipient;
	}

	public void setRecipient(final DbAccount recipient) {
		this.recipient = recipient;
	}

	public Long getAmount() {
		return this.amount;
	}

	public void setAmount(final Long amount) {
		this.amount = amount;
	}

	public Integer getMessageType() {
		return this.messageType;
	}

	public void setMessageType(final Integer messageType) {
		this.messageType = messageType;
	}

	public byte[] getMessagePayload() {
		return this.messagePayload;
	}

	public void setMessagePayload(final byte[] messagePayload) {
		this.messagePayload = messagePayload;
	}

	public Collection<DbMosaic> getMosaics() {
		return this.mosaics;
	}

	public void setMosaics(final Set<DbMosaic> mosaics) {
		this.mosaics = mosaics;
	}
}
