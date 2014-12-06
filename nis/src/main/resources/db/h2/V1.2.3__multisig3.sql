CREATE TABLE IF NOT EXISTS `multisigtransactions` (  
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL AUTO_INCREMENT,  
  `shortId` BIGINT NOT NULL,  
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `type` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66) NOT NULL,
  
  `blkIndex` INT NOT NULL, -- index inside block
  `orderId` INT NOT NULL,
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  `transferId` BIGINT,
  `importanceTransferId` BIGINT,
  `multisigSignerModificationId` BIGINT,

  PRIMARY KEY (`id`)
);
