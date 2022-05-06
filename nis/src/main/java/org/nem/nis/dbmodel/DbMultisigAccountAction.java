package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Base class holding information about the multisig action of an account
 */
@MappedSuperclass
@SuppressWarnings("unused")
public class DbMultisigAccountAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long accountId;

	private Integer type;

	private Long height;

	private Long transactionId;

	public Long getAccountId() {
		return this.accountId;
	}

	public void setAccountId(final Long accountId) {
		this.accountId = accountId;
	}

	public Integer getType() {
		return this.type;
	}

	public void setType(final Integer type) {
		this.type = type;
	}

	public Long getHeight() {
		return this.height;
	}

	public void setHeight(final Long height) {
		this.height = height;
	}

	public Long getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(final Long transactionId) {
		this.transactionId = transactionId;
	}
}
