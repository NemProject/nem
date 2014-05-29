package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.nis.controller.annotations.P2PApi;
import org.nem.nis.service.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * This controller will handle data propagation:
 * * /push/transaction - for what is now model.Transaction
 * * /push/block - for model.Block
 * <p/>
 * It would probably fit better in TransferController, but this is
 * part of p2p API, so I think it should be kept separated.
 * (I think it might pay off in future, if we'd like to add restrictions to client APIs)
 */

// TODO: add tests
@RestController
public class PushController {

	private final PushService pushService;

	@Autowired(required = true)
	public PushController(final PushService pushService) {
		this.pushService = pushService;
	}

	@RequestMapping(value = "/push/transaction", method = RequestMethod.POST)
	@P2PApi
	public void pushTransaction(@RequestBody final Deserializer deserializer, final HttpServletRequest request) {
		this.pushService.pushTransaction(TransactionFactory.VERIFIABLE.deserialize(deserializer), request);
	}

	@RequestMapping(value = "/push/block", method = RequestMethod.POST)
	@P2PApi
	public void pushBlock(@RequestBody final Deserializer deserializer, final HttpServletRequest request) {
		this.pushService.pushBlock(BlockFactory.VERIFIABLE.deserialize(deserializer), request);
	}
}
