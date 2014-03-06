package org.nem.nis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nem.core.dao.BlockDao;
import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.Account;
import org.nem.core.dbmodel.Block;
import org.nem.core.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountAnalyzer {
	@Autowired
	private BlockDao blockDao;
	
	@Autowired
	private TransferDao transferDao;
	
	private Map<byte[], Long> accountBalance;
	private Map<byte[], Long> accountUnconfirmedBalance;
	
	public AccountAnalyzer() {
		accountBalance = new HashMap<byte[], Long>();
		accountUnconfirmedBalance = new HashMap<byte[], Long>();
	}

	private void addToBalanceAndUnconfirmedBalance(Account a, long amount) {
		byte[] accountId = a.getPublicKey();
		if (! accountBalance.containsKey(accountId)) {
			accountBalance.put(accountId, 0L);
		}
		if (! accountUnconfirmedBalance.containsKey(accountId)) {
			accountUnconfirmedBalance.put(accountId, 0L);
		}
		
		accountBalance.put(accountId, accountBalance.get(accountId) + amount);
		accountUnconfirmedBalance.put(accountId, accountUnconfirmedBalance.get(accountId) + amount);
	}
	
	/*
	 * analyze block from db
	 * 
	 * if we're here it means that both block an it's transactions
	 * have been saved in db
	 */
	public void analyze(Block curBlock) {
		System.out.print("analyzing block: ");
		System.out.print(curBlock.getShortId());
		System.out.print(", #tx ");
		
		List<Transfer> txes = curBlock.getBlockTransfers();
		System.out.println(txes.size());
		
		for(Iterator<Transfer> i = txes.iterator(); i.hasNext(); ) {
			Transfer tx = i.next();
			
			addToBalanceAndUnconfirmedBalance(tx.getSender(), -(tx.getAmount() + tx.getFee()) * 100L);
			
			/*
			System.out.print(tx.getId());
			System.out.print(" ");
			System.out.print(tx.getBlkIndex());
			System.out.print(" ");
			System.out.print(tx.getShortId());
			System.out.print(" ");
			System.out.print(tx.getSender().getShortId());
			System.out.print(" ");
			System.out.print(tx.getRecipient().getShortId());
			System.out.println();
			*/
			
			switch (tx.getType())
			{
			case 0: 
				addToBalanceAndUnconfirmedBalance(tx.getRecipient(), tx.getAmount() * 100L);
				System.out.println(String.format("%d -> %d", tx.getRecipient().getId(), tx.getAmount()));
				break;
			}
		}
	}
}
