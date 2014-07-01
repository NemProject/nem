package org.nem.nis.mappers;

import org.nem.core.model.ValidationResult;
import org.nem.peer.NodeInteractionResult;

public class ValidationResultToNodeInteractionResultMapper {
	public static NodeInteractionResult map(final ValidationResult validationResult) {
		switch (validationResult) {
			case SUCCESS: return NodeInteractionResult.SUCCESS;
	        case NEUTRAL: return NodeInteractionResult.NEUTRAL;
	        default: return NodeInteractionResult.FAILURE;
		}
	}
}
