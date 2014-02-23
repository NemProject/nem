package org.nem.core.dbmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import javax.persistence.GenerationType;

@Entity  
@Table(name="accounts") 
public class Account {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
    // base32 public key encoded with org.nem.core.model.Address
	private String printableKey;
    // public key, might be null
	private byte[] publicKey;

	public Account() {}
	
	public Account(String printableKey, byte[] publicKey) {
		this.printableKey = printableKey;
		this.publicKey = publicKey;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPrintableKey() {
		return printableKey;
	}

	public void setPrintableKey(String printableKey) {
		this.printableKey = printableKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	
}
