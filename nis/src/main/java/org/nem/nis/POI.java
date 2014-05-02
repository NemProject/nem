package org.nem.nis;

import java.util.List;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;

/**
 * This is the interface for POI. We may want to try different specific
 * implementations, so I moved this out to make that easier.
 */
public interface POI {
	public ColumnVector getAccountImportances(List<Account> accounts);
}
