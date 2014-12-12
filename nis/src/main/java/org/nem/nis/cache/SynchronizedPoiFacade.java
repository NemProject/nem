package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.DebitPredicate;

import java.util.Iterator;

/**
 * A synchronized PoiFacade implementation.
 */
public class SynchronizedPoiFacade implements PoiFacade, CopyableCache<SynchronizedPoiFacade> {
	private final DefaultPoiFacade poiFacade;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param poiFacade The wrapped cache.
	 */
	public SynchronizedPoiFacade(final DefaultPoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}
	//
	//@Override
	//public Iterator<AccountState> iterator() {
	//	// TODO 20141211 this really isn't synchronized.
	//	synchronized (this.lock) {
	//		return this.poiFacade.iterator();
	//	}
	//}
	//
	//@Override
	//public AccountState findStateByAddress(final Address address) {
	//	synchronized (this.lock) {
	//		return this.poiFacade.findStateByAddress(address);
	//	}
	//}
	//
	//@Override
	//public AccountState findLatestForwardedStateByAddress(final Address address) {
	//	synchronized (this.lock) {
	//		return this.poiFacade.findLatestForwardedStateByAddress(address);
	//	}
	//}
	//
	//@Override
	//public AccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
	//	synchronized (this.lock) {
	//		return this.poiFacade.findForwardedStateByAddress(address, height);
	//	}
	//}
	//
	//@Override
	//public int size() {
	//	synchronized (this.lock) {
	//		return this.poiFacade.size();
	//	}
	//}

	@Override
	public int getLastPoiVectorSize() {
		synchronized (this.lock) {
			return this.poiFacade.getLastPoiVectorSize();
		}
	}

	@Override
	public BlockHeight getLastPoiRecalculationHeight() {
		synchronized (this.lock) {
			return this.poiFacade.getLastPoiRecalculationHeight();
		}
	}

	//@Override
	//public void removeFromCache(final Address address) {
	//	synchronized (this.lock) {
	//		this.poiFacade.removeFromCache(address);
	//	}
	//}
	//
	//@Override
	//public void undoVesting(final BlockHeight height) {
	//	synchronized (this.lock) {
	//		this.poiFacade.undoVesting(height);
	//	}
	//}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight) {
		synchronized (this.lock) {
			this.poiFacade.recalculateImportances(blockHeight);
		}
	}
	//
	//@Override
	//public DebitPredicate getDebitPredicate() {
	//	synchronized (this.lock) {
	//		// TODO 20141211 this really isn't synchronized.
	//		return this.poiFacade.getDebitPredicate();
	//	}
	//}

	//region CopyableCache

	@Override
	public void shallowCopyTo(final SynchronizedPoiFacade rhs) {
		synchronized (this.lock) {
			this.poiFacade.shallowCopyTo(rhs.poiFacade);
		}
	}

	@Override
	public SynchronizedPoiFacade copy() {
		synchronized (this.lock) {
			return new SynchronizedPoiFacade(this.poiFacade.copy());
		}
	}

	//endregion
}
