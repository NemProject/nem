package org.nem.nis.model;

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
	private byte[] printableKey;
	private byte[] publicKey;

	public Account() {}
	
	public Account(byte[] printableKey, byte[] publicKey) {
		this.printableKey = printableKey;
		this.publicKey = publicKey;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getPrintableKey() {
		return printableKey;
	}

	public void setPrintableKey(byte[] printableKey) {
		this.printableKey = printableKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	
}
