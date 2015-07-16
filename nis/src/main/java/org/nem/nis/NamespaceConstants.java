package org.nem.nis;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

import java.util.Properties;

/**
 * Constants used by namespace related classes.
 */
public class NamespaceConstants {
	private static final PublicKey NAMESPACE_OWNER_NEM_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");

	/**
	 * The 'nem' namespace owner.
	 */
	public static final Account NAMESPACE_OWNER_NEM = new Account(Address.fromPublicKey(NAMESPACE_OWNER_NEM_KEY));

	/**
	 * The 'nem' namespace id.
	 */
	public static final NamespaceId NAMESPACE_ID_NEM = new NamespaceId("nem");

	/**
	 * The 'nem' namespace.
	 */
	public static final Namespace NAMESPACE_NEM = new Namespace(NAMESPACE_ID_NEM, NAMESPACE_OWNER_NEM, BlockHeight.MAX);

	/**
	 * The 'nem.xem' mosaic.
	 */
	public static final Mosaic MOSAIC_XEM = createXemMosaic();

	/**
	 * The namespace entry for 'nem' that contains a single mosaic 'nem.xem'.
	 */
	public static final NamespaceEntry NAMESPACE_ENTRY_NEM = new NamespaceEntry(NAMESPACE_NEM, createNemMosaics());

	private static Mosaic createXemMosaic() {
		final MosaicId mosaicId = new MosaicId(NAMESPACE_ID_NEM, "xem");
		final MosaicDescriptor descriptor = new MosaicDescriptor("reserved xem mosaic");
		final Properties properties = new Properties();
		properties.put("divisibility", "6");
		properties.put("quantity", "8999999999000000");
		properties.put("mutablequantity", "false");
		properties.put("transferable", "true");
		return new Mosaic(
				NamespaceConstants.NAMESPACE_OWNER_NEM,
				mosaicId,
				descriptor,
				new DefaultMosaicProperties(properties));
	}

	private static Mosaics createNemMosaics() {
		final Mosaics mosaics = new Mosaics();
		final MosaicEntry mosaicEntry = mosaics.add(MOSAIC_XEM);
		mosaicEntry.increaseSupply(Quantity.fromValue(8_999_999_999_000_000L));
		return mosaics;
	}}
