CREATE TABLE IF NOT EXISTS `accounts` (  
  `id` BIGINT NOT NULL AUTO_INCREMENT,  

  `printableKey` VARCHAR(42) NOT NULL,

  `publicKey` VARBINARY(34), -- additional two bytes
  PRIMARY KEY (`id`)  
);  

CREATE TABLE IF NOT EXISTS `block_transfers` (  
  `block_id` BIGINT NOT NULL,
  `transfer_id` BIGINT NOT NULL,  
  PRIMARY KEY (`transfer_id`)
);  

CREATE TABLE IF NOT EXISTS `blocks` (  
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shortId` BIGINT NOT NULL,

  `version` INT NOT NULL,
  `prevBlockHash` VARBINARY(34) NOT NULL,
  `blockHash` VARBINARY(34) NOT NULL,
  `timestamp` INT NOT NULL,

  `forgerId` BIGINT NOT NULL, -- reference to account table
  `forgerProof` VARBINARY(66) NOT NULL,
  `blockSignature` VARBINARY(66) NOT NULL,

  `height` BIGINT NOT NULL,

  `totalAmount` BIGINT NOT NULL, -- probably it'll be better to keep it
  `totalFee` BIGINT NOT NULL,    --

  `nextBlockId` BIGINT, -- can be null, we should fill this when adding next block

  PRIMARY KEY (`id`)
);  
  
CREATE TABLE IF NOT EXISTS `transfers` (  
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
  `recipientId` BIGINT NOT NULL, -- reference to accounts

  `blkIndex` INT NOT NULL, -- index inside block
  `amount` BIGINT NOT NULL,
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.block_transfers ADD
  FOREIGN KEY (block_id)
  REFERENCES public.blocks(id);

ALTER TABLE public.block_transfers ADD
  FOREIGN KEY (transfer_id)
  REFERENCES public.transfers(id);

ALTER TABLE public.blocks ADD
  FOREIGN KEY (forgerId)
  REFERENCES public.accounts(id);

ALTER TABLE public.transfers ADD
  FOREIGN KEY (recipientId)
  REFERENCES public.accounts(id);

ALTER TABLE transfers ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

