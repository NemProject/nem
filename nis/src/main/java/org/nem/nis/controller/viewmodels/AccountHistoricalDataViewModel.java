package org.nem.nis.controller.viewmodels;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;

/**
 * A view model to display historical account data.
 */
public class AccountHistoricalDataViewModel implements SerializableEntity {
	private final BlockHeight height;
	private final Address address;
	private final Amount balance;
	private final Amount vestedBalance;
	private final Amount unvestedBalance;
	private final double importance;
	private final double pageRank;

	/**
	 * Creates a new account view model.
	 *
	 * @param height The height the data was taken from.
	 * @param address The address.
	 * @param balance The balance.
	 * @param vestedBalance The vested balance.
	 * @param unvestedBalance The unvested balance.
	 * @param importance The importance.
	 * @param pageRank The page rank.
	 */
	public AccountHistoricalDataViewModel(final BlockHeight height, final Address address, final Amount balance, final Amount vestedBalance,
			final Amount unvestedBalance, final double importance, final double pageRank) {
		this.height = height;
		this.address = address;
		this.balance = balance;
		this.vestedBalance = vestedBalance;
		this.unvestedBalance = unvestedBalance;
		this.importance = importance;
		this.pageRank = pageRank;
	}

	/**
	 * Gets the height.
	 *
	 * @return This height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets the account's address.
	 *
	 * @return This account's address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	/**
	 * Gets the account's vested balance.
	 *
	 * @return This account's vested balance.
	 */
	public Amount getVestedBalance() {
		return this.vestedBalance;
	}

	/**
	 * Gets the account's unvested balance.
	 *
	 * @return This account's unvested balance.
	 */
	public Amount getUnvestedBalance() {
		return this.unvestedBalance;
	}

	/**
	 * Gets the importance associated with this account.
	 *
	 * @return The importance associated with this account.
	 */
	public double getImportance() {
		return this.importance;
	}

	/**
	 * Gets the page rank associated with this account.
	 *
	 * @return The page rank associated with this account.
	 */
	public double getPageRank() {
		return this.pageRank;
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		Address.writeTo(serializer, "address", this.getAddress(), AddressEncoding.COMPRESSED);
		Amount.writeTo(serializer, "balance", this.getBalance());
		Amount.writeTo(serializer, "vestedBalance", this.getVestedBalance());
		Amount.writeTo(serializer, "unvestedBalance", this.getUnvestedBalance());
		serializer.writeDouble("importance", this.getImportance());
		serializer.writeDouble("pageRank", this.getPageRank());
	}
}
