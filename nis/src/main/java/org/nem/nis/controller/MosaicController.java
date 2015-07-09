package org.nem.nis.controller;

import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyMosaicDao;
import org.nem.nis.dbmodel.DbMosaic;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * REST mosaic controller.
 */
@RestController
public class MosaicController {
	private final ReadOnlyMosaicDao mosaicDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	MosaicController(
			final ReadOnlyMosaicDao mosaicDao,
			final NisDbModelToModelMapper mapper) {
		this.mosaicDao = mosaicDao;
		this.mapper = mapper;
	}

	//region getMosaics

	/**
	 * Gets all known mosaics.
	 *
	 * @param pageBuilder The page builder.
	 * @return All known mosaics.
	 */
	@RequestMapping(value = "/mosaics", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicMetaDataPair> getMosaics(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaic> mosaics = this.mosaicDao.getMosaics(page.getId(), page.getPageSize());
		final Collection<MosaicMetaDataPair> pairs = mosaics.stream()
				.map(n -> new MosaicMetaDataPair(this.mapper.map(n), new DefaultMetaData(n.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

}
