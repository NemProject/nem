package org.nem.nis;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.Supply;
import org.nem.nis.state.*;

import java.util.*;

/**
 * Nem namespace related classes.
 */
public class NemNamespaceEntry {

	/**
	 * The namespace entry for 'nem' that contains a single mosaic 'nem.xem'.
	 */
	private static NamespaceEntry instance;

	private static Mosaics createNemMosaics(final BlockHeight mosaicRedefinitionForkHeight) {
		final MosaicEntry mosaicEntry = new MosaicEntry(MosaicConstants.MOSAIC_DEFINITION_XEM);
		return new UnmodifiableMosaics(Collections.singletonList(mosaicEntry), mosaicRedefinitionForkHeight);
	}

	private static class UnmodifiableMosaics extends Mosaics {

		public UnmodifiableMosaics(final Collection<MosaicEntry> mosaics, final BlockHeight mosaicRedefinitionForkHeight) {
			super(MosaicConstants.NAMESPACE_ID_NEM, mosaicRedefinitionForkHeight);
			mosaics.forEach(entry -> super.add(new UnmodifiableMosaicEntry(entry)));
		}

		@Override
		public void add(final MosaicEntry entry) {
			throw new UnsupportedOperationException("add is not allowed");
		}

		@Override
		public MosaicEntry remove(final MosaicId mosaic) {
			throw new UnsupportedOperationException("remove is not allowed");
		}
	}

	private static class UnmodifiableMosaicEntry extends MosaicEntry {

		public UnmodifiableMosaicEntry(final MosaicEntry entry) {
			super(entry.getMosaicDefinition(), entry.getSupply());
		}

		@Override
		public void increaseSupply(final Supply increase) {
			throw new UnsupportedOperationException("increaseSupply is not allowed");
		}

		@Override
		public void decreaseSupply(final Supply decrease) {
			throw new UnsupportedOperationException("decreaseSupply is not allowed");
		}
	}

	/**
	 * Gets the namespace entry for 'nem' that contains a single mosaic 'nem.xem'.
	 *
	 * @param mosaicRedefinitionForkHeight The mosaic redefinition fork height.
	 * @return The namespace entry.
	 */
	public static NamespaceEntry getInstance(final BlockHeight mosaicRedefinitionForkHeight) {
		if (null == instance) {
			instance = new NamespaceEntry(MosaicConstants.NAMESPACE_NEM,
					createNemMosaics(mosaicRedefinitionForkHeight));
		}

		return instance;
	}
}
