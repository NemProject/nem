package org.nem.nis.mappers;

import org.nem.core.model.Block;

/**
 * A NIS mapper facade for mapping db model types to model types.
 */
public class NisDbModelToModelMapper {
	private final IMapper mapper;

	/**
	 * Creates a mapper facade.
	 *
	 * @param mapper The mapper.
	 */
	public NisDbModelToModelMapper(final IMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Maps a db model block to a model block.
	 *
	 * @param block The db model block.
	 * @return The model block.
	 */
	public Block map(final org.nem.nis.dbmodel.Block block) {
		return this.mapper.map(block, Block.class);
	}
}