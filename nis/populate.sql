CREATE TABLE IF NOT EXISTS `accounts` (  
  `id` BIGINT NOT NULL AUTO_INCREMENT,  

  `printableKey` VARBINARY(52) NOT NULL, -- for testing purposes

  `publicKey` VARBINARY(32),
  PRIMARY KEY (`id`)  
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;  

CREATE TABLE IF NOT EXISTS `transfers` (  
  `id` BIGINT NOT NULL AUTO_INCREMENT,  
  `shortId` BIGINT NOT NULL,  

  `version` INT NOT NULL,
  `type` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(64) NOT NULL,
  `recipientId` BIGINT NOT NULL, -- reference to accounts

  `blkIndex` INT NOT NULL, -- index inside block
  `amount` BIGINT NOT NULL,
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`),
  KEY `accounts` (`senderId`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;  

CREATE TABLE IF NOT EXISTS `blocks` (  
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shortId` BIGINT NOT NULL,

  `version` INT NOT NULL,
  `prevBlockHash` VARBINARY(32) NOT NULL,
  `blockHash` VARBINARY(32) NOT NULL,
  `timestamp` INT NOT NULL,

  `forgerId` BIGINT NOT NULL, -- reference to account table
  `forgerProof` VARBINARY(64) NOT NULL,
  `blockSignature` VARBINARY(64) NOT NULL,

  `height` BIGINT NOT NULL,

  `totalAmount` INT NOT NULL, -- probably it'll be better to keep it
  `totalFee` INT NOT NULL,    --

  `nextBlockId` BIGINT, -- can be null, we should fill this when adding next block

  PRIMARY KEY (`id`), 
  KEY `accounts` (`forgerId`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;  
  
CREATE TABLE IF NOT EXISTS `block_transfers` (  
  `block_id` BIGINT NOT NULL,
  `transfer_id` BIGINT NOT NULL,  
  KEY `blocks` (`block_id`),
  KEY `transfers` (`transfer_id`)  
) ENGINE=InnoDB DEFAULT CHARSET=utf8;  

