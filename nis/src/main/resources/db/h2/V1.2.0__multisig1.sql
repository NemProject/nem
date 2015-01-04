CREATE TABLE IF NOT EXISTS `multisigsignermodifications` (  
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL AUTO_INCREMENT,  
  `shortId` BIGINT NOT NULL,  
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66) NOT NULL,

  `blkIndex` INT NOT NULL, -- index inside block
  `orderId` INT NOT NULL,
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigsignermodifications ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.multisigsignermodifications ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

CREATE INDEX IDX_MULTISIGSIGNERMODIFICATIONS_TIMESTAMP ON `multisigsignermodifications` (timeStamp);
CREATE INDEX IDX_MULTISIGSIGNERMODIFICATIONS_SENDERID ON `multisigsignermodifications` (senderId, id desc);

CREATE TABLE IF NOT EXISTS `multisigmodifications` (
  `multisigSignerModificationId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cosignatoryId` BIGINT NOT NULL, -- reference to accounts
  `modificationType` INT NOT NULL, -- create / destroy

  PRIMARY KEY(`id`)
);

ALTER TABLE public.multisigmodifications ADD
  FOREIGN KEY (multisigSignerModificationId)
  REFERENCES public.multisigsignermodifications(id);

ALTER TABLE public.multisigmodifications ADD
  FOREIGN KEY (cosignatoryId)
  REFERENCES public.accounts(id);

CREATE INDEX IDX_MULTISIGMODIFICATIONS_COSIGNATORYID ON `multisigmodifications` (cosignatoryId);

