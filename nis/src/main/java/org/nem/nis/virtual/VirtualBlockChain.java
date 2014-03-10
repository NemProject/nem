package org.nem.nis.virtual;

import java.util.LinkedList;

import org.nem.nis.NisMain;
import org.nem.core.dbmodel.Account;
import org.nem.core.dbmodel.Block;
import org.nxt.nrs.Crypto;
import org.nxt.nrs.NrsBlock;


public class VirtualBlockChain {
	public VirtualBlockChain(Block initialBlock) {
		m_blockChain = new LinkedList<Block>();
		
		m_blockChain.add(initialBlock);
	}
	
	public boolean verifyGenerationSignature(Block prev, Block act, Account generator) {
		if (act.getVersion() == 1 && !(Crypto.verify(act.getForgerProof(), prev.getForgerProof(), generator.getPublicKey()))) {
			System.out.println("SIGNATURE DOES NOT MATCH");
			return false;
		}
		System.out.println("SIGNATURE MATCHES");
		
		return true;
	}
	public boolean add(NrsBlock nrsBlock, Block block, Account generator) {
		int curTime = NisMain.TIME_PROVIDER.getCurrentTime();
		Block lastVirtBlock = m_blockChain.getLast();
		
		System.out.print("yo, ho, hello there ");
		System.out.println( lastVirtBlock.getHeight() );
		System.out.println( block.getTimestamp() );
		System.out.println( lastVirtBlock.getTimestamp() );
		System.out.println( curTime );
		
		Integer version = block.getVersion();
		if (version != (lastVirtBlock.getHeight() < 30000 ? 1 : 2)) {
            return false;
        }
		
		if (lastVirtBlock.getHeight() == 30000) {
			throw new RuntimeException("v2 not handled yet");
		}
		
		
		if ((block.getTimestamp() > curTime + 15) || (block.getTimestamp() <= lastVirtBlock.getTimestamp())) {
			return false;
		}
		
		int payloadLength = nrsBlock.getPayloadLength();
		if ((payloadLength > 32640) || /*(224 + payloadLength != buffer.capacity()) ||*/ (nrsBlock.getNumberOfTransactions() > 255)) {
            return false;
        }
		
		if (/*(block.transactions.length > 255) ||*/ (nrsBlock.getPreviousBlock() != lastVirtBlock.getShortId()) || (block.getShortId() == 0L)) {
			return false;
		}

		System.out.println("block ok");
		
		//|| (Nxt.blocks.get(Long.valueOf(block.getId())) != null)
//		if (blockDao.findByShortId(nrsBlock.getId()) != null) {
//			throw new RuntimeException("handle already present block");
//		}
		
		if (! verifyGenerationSignature(lastVirtBlock, block, generator)) {
			return false;
		}
		// || (!block.verifyGenerationSignature()) || (!block.verifyBlockSignature())) {
		m_blockChain.add(block);
		return true;
	}
	
	private LinkedList<Block> m_blockChain;
}
