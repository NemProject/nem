package org.nem.nis;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.BlockHeight;
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
	public static NamespaceEntry NAMESPACE_ENTRY_NEM;

	static {
		setNamespaceEntry();
	}

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

	private static void setNamespaceEntry() {
		final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
		setNamespaceEntry(forkConfiguration.getMosaicRedefinitionForkHeight());
	}

	/**
	 * Sets the namespace entry for 'nem' that contains a single mosaic 'nem.xem'.
	 *
	 * @param mosaicRedefinitionForkHeight The mosaic redefinition fork height.
	 */
	public static void setNamespaceEntry(final BlockHeight mosaicRedefinitionForkHeight) {
		NAMESPACE_ENTRY_NEM = new NamespaceEntry(MosaicConstants.NAMESPACE_NEM, createNemMosaics(mosaicRedefinitionForkHeight));
	}
}
