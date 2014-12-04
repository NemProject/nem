-- forgotten in previous sql

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

CREATE INDEX IDX_MULTISIGTRANSACTIONS_SHORT_ID ON `multisigtransactions` (shortId);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_TRANSFERHASH ON `multisigtransactions` (transferHash);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_TIMESTAMP ON `multisigtransactions` (timeStamp);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_SENDERID ON `multisigtransactions` (senderId);

-- actual ones from this file

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

CREATE INDEX IDX_MULTISIGSIGNATURES_SHORT_ID ON `multisigsignatures` (shortId);
CREATE INDEX IDX_MULTISIGSIGNATURES_TRANSFERHASH ON `multisigsignatures` (transferHash);
CREATE INDEX IDX_MULTISIGSIGNATURES_TIMESTAMP ON `multisigsignatures` (timeStamp);
CREATE INDEX IDX_MULTISIGSIGNATURES_SENDERID ON `multisigsignatures` (senderId);

