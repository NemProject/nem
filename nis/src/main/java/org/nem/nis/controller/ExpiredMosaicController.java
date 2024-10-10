package org.nem.nis.controller;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.SerializableList;
import org.nem.specific.deploy.NisConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.BlockHeightBuilder;
import org.nem.nis.controller.viewmodels.ExpiredMosaicViewModel;
import org.nem.nis.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.*;

/**
 * REST expired mosaic controller.
 */
@RestController
public class ExpiredMosaicController {
	private final NisConfiguration nisConfiguration;
	private final ReadOnlyExpiredMosaicCache expiredMosaicCache;

	@Autowired(required = true)
	ExpiredMosaicController(final NisConfiguration nisConfiguration, final ReadOnlyExpiredMosaicCache expiredMosaicCache) {
		this.nisConfiguration = nisConfiguration;
		this.expiredMosaicCache = expiredMosaicCache;
	}

	// region expiredMosaics

	/**
	 * Gets the expired mosaics at the specified height.
	 *
	 * @param heightBuilder The height builder.
	 * @return The expired mosaics (or empty if not found).
	 */
	@RequestMapping(value = "/local/mosaics/expired", method = RequestMethod.GET)
	@ClientApi
	@TrustedApi
	public SerializableList<ExpiredMosaicViewModel> expiredMosaics(final BlockHeightBuilder heightBuilder) {
		if (!this.nisConfiguration.isFeatureSupported(NodeFeature.TRACK_EXPIRED_MOSAICS)) {
			throw new UnsupportedOperationException("this node does not support tracking expired mosaics");
		}

		final BlockHeight height = heightBuilder.build();
		final Collection<ExpiredMosaicEntry> expirations = this.expiredMosaicCache.findExpirationsAtHeight(height);
		return new SerializableList<>(
				expirations.stream().map(expiration -> new ExpiredMosaicViewModel(expiration)).collect(Collectors.toList()));
	}

	// endregion
}
