package org.nem.nis.cache;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

import java.util.Properties;

/**
 * Constants used by namespace related classes.
 * TODO 20150715 J-B: i don't think this belongs in the cache package
 * TODO 20150716 BR -> J: right, i didn't know where to put it. Feel free to move it elsewhere.
 */
public class NamespaceConstants {
	private static final PublicKey LESSOR_PUBLIC_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	// TODO 20150715 J-B: do all of these need to be public?
	// TODO 20150716 BR -> J: they are used by other classes (NAMESPACE_NEM is only used in test class).
	public static final Account LESSOR = new Account(Address.fromPublicKey(LESSOR_PUBLIC_KEY));
	public static final NamespaceId NAMESPACE_ID_NEM = new NamespaceId("nem");
	public static final Namespace NAMESPACE_NEM = new Namespace(NAMESPACE_ID_NEM, LESSOR, BlockHeight.MAX);
	public static final Mosaic MOSAIC_XEM = createXemMosaic();
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
				NamespaceConstants.LESSOR,
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
