package org.nem.nis.state;

import org.nem.core.model.primitive.*;

public interface ReadOnlyAccountInfo {
	Amount getBalance();

	BlockAmount getHarvestedBlocks();

	String getLabel();

	ReferenceCount getReferenceCount();
}
