package org.nem.nis.dbmodel;

import org.nem.core.crypto.PublicKey;

import javax.persistence.*;

/**
 * DbAccount entity.
 * <br>
 * Probably it should be called Address, as it's main purpose is to associate
 * printableKey with publicKey.
 * <br>
 * In future it should probably also two 'heights' of an DbAccount,
 * marking at what blockchain height has network 'learned' about
 * DbAccount NEM address (printableKey) and public key respectively.
 */
@Entity
@Table(name = "accounts")
public class DbAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	// base32 public key encoded with org.nem.core.model.Address
	private String printableKey;
	// public key, might be null
	private byte[] publicKey;

	public DbAccount() {
	}

	public DbAccount(final String printableKey, final PublicKey publicKey) {
		this.printableKey = printableKey;
		this.setPublicKey(publicKey);
	}

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getPrintableKey() {
		return this.printableKey;
	}

	public void setPrintableKey(final String printableKey) {
		this.printableKey = printableKey;
	}

	public PublicKey getPublicKey() {
		return null == this.publicKey ? null : new PublicKey(this.publicKey);
	}

	public void setPublicKey(final PublicKey publicKey) {
		if (null != publicKey) {
			this.publicKey = publicKey.getRaw();
		}
	}
}
