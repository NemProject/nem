package org.nem.core.dbmodel;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.nis.AccountAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Translator {

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	public org.nem.core.model.Block convert(Block block) {
		org.nem.core.model.Account forger = accountAnalyzer.findByAddress(Address.fromPublicKey(block.getForger().getPublicKey()));
		org.nem.core.model.Block res = new org.nem.core.model.Block(
				forger,
				block.getPrevBlockHash(),
				new TimeInstant(block.getTimestamp()),
				block.getHeight()
		);
		res.setSignature(new Signature(block.getForgerProof()));
		for (Transfer transfer : block.getBlockTransfers()) {
			org.nem.core.model.Account sender = accountAnalyzer.findByAddress(Address.fromPublicKey(transfer.getSender().getPublicKey()));
			org.nem.core.model.Account recipient = accountAnalyzer.findByAddress(Address.fromEncoded(transfer.getRecipient().getPrintableKey()));
			TransferTransaction transferTransaction = new TransferTransaction(
					new TimeInstant(transfer.getTimestamp()),
					sender,
					recipient,
					transfer.getAmount(),
					null
			);
			transferTransaction.setFee(transfer.getFee());
			transferTransaction.setDeadline(new TimeInstant(transfer.getDeadline()));
			transferTransaction.setSignature(new Signature(transfer.getSenderProof()));
			res.addTransaction(transferTransaction);
		}
		return res;
	}
}
