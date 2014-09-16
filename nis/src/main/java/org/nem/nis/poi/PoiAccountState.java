package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.CircularStack;
import org.nem.nis.secret.*;

/**
 * Class containing extrinsic NIS-account information that is used to calculate POI.
 * TODO-CR 20140808 J->ALL i think the naming is confusing between PoiAccountState and PoiAccountInfo ... should try to think of better names
 * G->J, I think we can safely call it AccountState, can't we?
 * TODO 20140909 J-G: seems reasonable
 * TODO 20140915 J-G: do you mind doing the rename?
 */
public class PoiAccountState {
	private static final int REMOTE_STATE_SIZE = 2;

	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private BlockHeight height;

	// The following rules will apply:
	//  1. one will have to wait 1440 blocks to  activate/deactivate remote account (before it'll become operational)
	//  2. in cannot make two SAME subsequent announcements: so let's say I've  announce address X as my remote address.
	//    now if I want to announce address Y. I first need to cancel/deactivate address X first.
	//
	// This makes whole logic a lot simpler

	// I thought we're gonna need 3 elements, but it would work with 2.
	//    1. let's say there is alias created at block 1000, it becomes effective at block 2440
	//    2. now at block 2500 user removes alias, removal will become effective at block 3940
	//    3. user won't be able to create new alias before 3940, so there is no need, for this to have 3 elements
	//        as eventual (blockchain) rollback won't change anything, so I'm changing REMOTE_STATE_SIZE to 2
	private CircularStack<RemoteState> remoteStateStack;

	/**
	 * Creates a new NIS account state.
	 */
	public PoiAccountState(final Address address) {
		this(address, new AccountImportance(), new WeightedBalances(), null);
	}

	private PoiAccountState(
			final Address address,
			final AccountImportance importance,
			final WeightedBalances weightedBalances,
			final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.weightedBalances = weightedBalances;
		this.height = height;
	}

	/**
	 * Gets the account address.
	 *
	 * @return The account address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the weighted balances.
	 *
	 * @return The weighted balances.
	 */
	public WeightedBalances getWeightedBalances() {
		return this.weightedBalances;
	}

	/**
	 * Gets the importance information.
	 *
	 * @return The importance information.
	 */
	public AccountImportance getImportanceInfo() {
		return this.importance;
	}

	/**
	 * Returns height of an account.
	 *
	 * @return The height of an account - when the account has been created.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Sets height of an account if the account does not already have a height.
	 *
	 * @param height The height.
	 */
	public void setHeight(final BlockHeight height) {
		if (null == this.height) {
			this.height = height;
		}
	}

	private void createStack() {
		if (this.remoteStateStack == null) {
			this.remoteStateStack = new CircularStack<>(REMOTE_STATE_SIZE);
		}
	}

	// TODO 20140909 J-G: i would definitely add tests for these
	// I know many tests are missing, I was adding quite some code in hope,
	// that I'll have it working after (6-7 Sep) weekend.
	// TODO 20140914 J-G: ok, but generally, i usually prefer to slip the date rather than sacrifice code quality / tests
	// because once code is checked in, it tends to stay around :)

	/**
	 * Creates association between "remote" account and "owner" account.
	 *  @param address Address of an owner account.
	 * @param height Height where association was created.
	 * @param direction Direction of an association.
	 */
	public void remoteFor(final Address address, final BlockHeight height, int direction) {
		createStack();
		this.remoteStateStack.push(new RemoteState(address, height, direction, true));
	}

	/**
	 * Creates association between "current" (owner) account and "remote" account.
	 *  @param address Address of remote account.
	 * @param height Height where association was created.
	 * @param direction Direction of an association.
	 */
	public void setRemote(final Address address, final BlockHeight height, final int direction) {
		createStack();
		this.remoteStateStack.push(new RemoteState(address, height, direction, false));
	}

	/**
	 * Removes association between "owner" and "remote".
	 *
	 * @param address Address of the account of other side of association.
	 * @param height Height where association was created.
	 * @param direction Direction of an association.
	 */
	public void resetRemote(final Address address, final BlockHeight height, final int direction) {
		// TODO: probably call to createStack should be removed from here
		createStack();

		// between changes of remoteState there must be 1440 blocks
		if (this.remoteStateStack.peek().getDirection() != direction ||
				!this.remoteStateStack.peek().getRemoteAddress().equals(address) ||
				!this.remoteStateStack.peek().getRemoteHeight().equals(height)) {
			throw new IllegalArgumentException("call to resetRemote must be 'paired' with call to remoteFor or setRemote");
		}
		this.remoteStateStack.remove();
	}

	/**
	 * Gets state for remote.
	 *
	 * @return Remote state if account have one.
	 */
	public RemoteState getRemoteState() {
		// TODO: probably call to createStack should be removed from here
		createStack();
		return this.remoteStateStack.peek();
	}

	/**
	 * Returns true if there is any remote state.
	 *
	 * @return True if there is any remote state, false otherwise.
	 */
	public boolean hasRemoteState() {
		return this.remoteStateStack != null && this.remoteStateStack.size() != 0;
	}

	/**
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		final PoiAccountState ret = new PoiAccountState(this.address, this.importance.copy(), this.weightedBalances.copy(), this.height);
		if (this.remoteStateStack != null) {
			this.remoteStateStack.shallowCopyTo(ret.remoteStateStack);
		}

		return ret;
	}
}