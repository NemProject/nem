package org.nem.nis;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Supply;
import org.nem.nis.state.*;

import java.util.*;

/**
 * Constants used by namespace related classes.
 */
public class NamespaceConstants {

	/**
	 * The namespace entry for 'nem' that contains a single mosaic 'nem.xem'.
	 */
	public static final NamespaceEntry NAMESPACE_ENTRY_NEM = new NamespaceEntry(MosaicConstants.NAMESPACE_NEM, createNemMosaics());

	private static Mosaics createNemMosaics() {
		final MosaicEntry mosaicEntry = new MosaicEntry(MosaicConstants.MOSAIC_DEFINITION_XEM);
		return new UnmodifiableMosaics(Collections.singletonList(mosaicEntry));
	}

	private static class UnmodifiableMosaics extends Mosaics {

		public UnmodifiableMosaics(final Collection<MosaicEntry> mosaics) {
			super(MosaicConstants.NAMESPACE_ID_NEM);
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
}
