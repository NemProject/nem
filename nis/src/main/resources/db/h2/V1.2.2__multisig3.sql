CREATE TABLE IF NOT EXISTS `multisigsignatures` (  
  `multisigTransactionId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL AUTO_INCREMENT,  
  `shortId` BIGINT NOT NULL,  
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66) NOT NULL,
  
  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigsignatures ADD
  FOREIGN KEY (multisigTransactionId)
  REFERENCES public.multisigtransactions(id);

ALTER TABLE public.multisigsignatures ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

CREATE INDEX IDX_MULTISIGSIGNATURES_TIMESTAMP ON `multisigsignatures` (timeStamp);
CREATE INDEX IDX_MULTISIGSIGNATURES_SENDERID ON `multisigsignatures` (senderId, id);

