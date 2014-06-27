package org.nem.nis.controller;

import org.nem.core.model.Transaction;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.Foraging;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.viewmodels.AccountPage;
import org.nem.nis.controller.viewmodels.AccountPageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionsController {

	private final Foraging foraging;

	@Autowired(required = true)
	public TransactionsController(final Foraging foraging) {
		this.foraging = foraging;
	}

	@RequestMapping(value = "/transactions/unconfirmed", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Transaction> transactionsUnconfirmed(final AccountPageBuilder builder) {
		final AccountPage page = builder.build();
		return new SerializableList<>(foraging.getUnconfirmedTransactions(page.getAddress()));
	}

}
