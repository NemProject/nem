package org.nem.nis.mappers;

import org.nem.core.model.Block;
import org.nem.nis.dbmodel.DbBlock;

/**
 * A NIS mapper facade for mapping model types to db model types.
 */
public class NisModelToDbModelMapper {
	private final IMapper mapper;

	/**
	 * Creates a mapper facade.
	 *
	 * @param mapper The mapper.
	 */
	public NisModelToDbModelMapper(final IMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Maps a model block to a db model block.
	 *
	 * @param block The model block.
	 * @return The db model block.
	 */
	public DbBlock map(final Block block) {
		return this.mapper.map(block, DbBlock.class);
	}
}
