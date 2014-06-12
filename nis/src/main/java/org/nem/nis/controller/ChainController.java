package org.nem.nis.controller;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import org.nem.core.crypto.HashChain;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.BlockScorer;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.controller.viewmodels.TransactionDebugInfo;
import org.nem.nis.service.RequiredBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChainController {

	private AccountLookup accountLookup;
	private RequiredBlockDao blockDao;
	private BlockChainLastBlockLayer blockChainLastBlockLayer;
	private BlockChain blockChain;

	@Autowired(required = true)
	public ChainController(
			final RequiredBlockDao blockDao, 
			final AccountLookup accountLookup, 
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockChain blockChain) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockChain = blockChain;
	}

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockLast() {
		return BlockMapper.toModel(this.blockChainLastBlockLayer.getLastDbBlock(), this.accountLookup);
	}

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	public SerializableList<Block> blocksAfter(@RequestBody final BlockHeight height) {
		// TODO: add tests for this action
		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(height);
		final SerializableList<Block> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		for (int i = 0; i < BlockChainConstants.BLOCKS_LIMIT; ++i) {
			Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountLookup));
		}

		return blockList;
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	public HashChain hashesFrom(@RequestBody final BlockHeight height) {
		return this.blockDao.getHashesFrom(height, BlockChainConstants.BLOCKS_LIMIT);
	}

	@RequestMapping(value = "/chain/score", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public BlockChainScore chainScore() {
		return this.blockChain.getScore();
	}

	/**
	 * Gets debug information about the block with the specified height.
	 *
	 * @param height The height.
	 * @return The matching block debug information
	 */
	@RequestMapping(value = "/chain/block-debug-info/get", method = RequestMethod.GET)
	@PublicApi
	public BlockDebugInfo blockDebugInfo(@RequestParam(value = "height") final String height) {
		final BlockHeight blockHeight = new BlockHeight(Long.parseLong(height));
		final AccountAnalyzer accountAnalyzer = this.blockChain.getAccountAnalyzerCopy();
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		final Block block = BlockMapper.toModel(dbBlock, accountAnalyzer);
		final org.nem.nis.dbmodel.Block dbParent = blockHeight.getRaw() == 1? null : this.blockDao.findByHeight(blockHeight.prev());
		final Block parent = blockHeight.getRaw() == 1? null : BlockMapper.toModel(dbParent, accountAnalyzer);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);
		scorer.forceImportanceCalculation();
		final BigInteger hit = scorer.calculateHit(block);
		final BigInteger target = parent == null? BigInteger.ZERO : scorer.calculateTarget(parent, block);
		final int interBlockTime = parent == null? 0 : block.getTimeStamp().subtract(parent.getTimeStamp());
		final BlockDebugInfo blockDebugInfo =  new BlockDebugInfo(
				block.getHeight(),
				block.getTimeStamp(),
				block.getSigner().getAddress(),
				block.getDifficulty(),
				hit,
				target,
				interBlockTime);
		
		for (Transaction transaction : block.getTransactions()) {
			Address recipient = transaction instanceof TransferTransaction? ((TransferTransaction)transaction).getRecipient().getAddress() : Address.fromEncoded("N/A");
			Amount amount = transaction instanceof TransferTransaction? ((TransferTransaction)transaction).getAmount() : Amount.fromMicroNem(0);
			String messageText = "";
			if (transaction.getType() == TransactionTypes.TRANSFER) {
				Message message = ((TransferTransaction)transaction).getMessage();
				if (message != null && message.canDecode()) {
					try {
						messageText = new String(message.getDecodedPayload(), "UTF-8");
					} catch (UnsupportedEncodingException e) {
					}
				}
			}
			TransactionDebugInfo transactionDebugInfo = new TransactionDebugInfo(
					transaction.getTimeStamp(),
					transaction.getDeadline(),
					transaction.getSigner().getAddress(),
					recipient,
					amount,
					transaction.getFee(),
					messageText);
			blockDebugInfo.addTransactionDebugInfo(transactionDebugInfo);
		}
		
		return blockDebugInfo;
	}
}
