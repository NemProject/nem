package org.nem.nis.sync;

import org.nem.core.model.ValidationResult;
import org.nem.core.model.primitive.BlockChainScore;

public class UpdateChainResult {
	public ValidationResult validationResult;
	public BlockChainScore ourScore;
	public BlockChainScore peerScore;
}
