
--CREATE TABLE IF NOT EXISTS `block_importancetransfers` (  
--  `order_index` INT NOT NULL,
--  `block_id` BIGINT NOT NULL,
--  `importancetransfer_id` BIGINT NOT NULL,  
--  PRIMARY KEY (`importancetransfer_id`)
--);  

CREATE TABLE IF NOT EXISTS `importancetransfers` (  
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
  `remoteId` BIGINT NOT NULL, -- reference to accounts
  `direction` INT NOT NULL, -- create / destroy

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

--ALTER TABLE public.block_importancetransfers ADD
--  FOREIGN KEY (block_id)
--  REFERENCES public.blocks(id);

--ALTER TABLE public.block_importancetransfers ADD
--  FOREIGN KEY (importancetransfer_id)
--  REFERENCES public.importancetransfers(id);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (remoteId)
  REFERENCES public.accounts(id);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

