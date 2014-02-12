package org.nem.nis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.model.Block;
import org.nem.nis.model.Transfer;
import org.springframework.beans.factory.annotation.Autowired;

public class BlockAnalyzer {
	@Autowired
	private BlockDao blockDao;
	
	@Autowired
	private TransferDao transactionDao;
	
	public BlockAnalyzer() {
		
	}

	public void analyze(Block curBlock) {
		System.out.print("analyzing block: ");
		System.out.println(curBlock.getShortId());
		
		List<Transfer> txes = curBlock.getBlockTransfers();
		System.out.println(txes.size());
		
		for(Iterator<Transfer> i = txes.iterator(); i.hasNext(); ) {
			Transfer tx = i.next();
			System.out.print(tx.getId());
			System.out.print(" ");
			System.out.print(tx.getBlkIndex());
			System.out.print(" ");
			System.out.print(tx.getShortId());
			System.out.print(" ");
			System.out.print(Converter.bytesToString(tx.getRecipient().getPrintableKey()));
			System.out.println();
		}
	}
}
