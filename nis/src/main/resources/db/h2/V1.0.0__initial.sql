CREATE TABLE IF NOT EXISTS `accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,

  `printableKey` VARCHAR(42) NOT NULL,

  `publicKey` VARBINARY(34), -- additional two bytes
  PRIMARY KEY (`id`)
);

--
-- BLOCKS
--

CREATE TABLE IF NOT EXISTS `blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,

  `version` INT NOT NULL,
  `prevBlockHash` VARBINARY(34) NOT NULL,
  `blockHash` VARBINARY(34) NOT NULL,
  `generationHash` VARBINARY(34) NOT NULL,
  `timestamp` INT NOT NULL,

  `harvesterId` BIGINT NOT NULL, -- reference to account table
  `harvesterProof` VARBINARY(66) NOT NULL,
  `harvestedInName` BIGINT,

  `height` BIGINT NOT NULL,

  `totalFee` BIGINT NOT NULL,    --
  `difficulty` BIGINT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.blocks ADD
  FOREIGN KEY (harvesterId)
  REFERENCES public.accounts(id);

ALTER TABLE public.blocks ADD
  FOREIGN KEY (harvestedInName)
  REFERENCES public.accounts(id);

-- sequence for the id values in the transaction tables
CREATE SEQUENCE transaction_id_seq;

--
-- TRANSFERS
--

CREATE TABLE IF NOT EXISTS `transfers` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `recipientId` BIGINT NOT NULL, -- reference to accounts

  `blkIndex` INT NOT NULL, -- index inside block
  `amount` BIGINT NOT NULL,
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  `messageType` INT,
  `messagePayload` VARBINARY(98),

  PRIMARY KEY (`id`)
);

ALTER TABLE public.transfers ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.transfers ADD
  FOREIGN KEY (recipientId)
  REFERENCES public.accounts(id);

ALTER TABLE transfers ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

--
-- IMPORTANCE TRANSFERS
--

CREATE TABLE IF NOT EXISTS `importancetransfers` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `remoteId` BIGINT NOT NULL, -- reference to accounts
  `mode` INT NOT NULL, -- create / destroy

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (remoteId)
  REFERENCES public.accounts(id);

ALTER TABLE public.importancetransfers ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

---
--- MULTISIG (signer modifications)
---

CREATE TABLE IF NOT EXISTS `multisigsignermodifications` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigsignermodifications ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.multisigsignermodifications ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

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

---
--- MULTISIG (transaction)
---

CREATE TABLE IF NOT EXISTS `multisigtransactions` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66) NOT NULL,

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  `transferId` BIGINT,
  `importanceTransferId` BIGINT,
  `multisigSignerModificationId` BIGINT,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (senderId)
  REFERENCES accounts(id);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (transferid)
  REFERENCES transfers(id);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (importancetransferid)
  REFERENCES importancetransfers(id);

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (multisigsignermodificationid)
  REFERENCES multisigsignermodifications(id);


CREATE TABLE IF NOT EXISTS `multisigsignatures` (
  `multisigTransactionId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
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

---

CREATE TABLE IF NOT EXISTS `multisigsends` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,

  `accountId` BIGINT NOT NULL,
  `type` INT NOT NULL,
  `height` BIGINT NOT NULL,
  `transactionId` BIGINT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigsends ADD
  FOREIGN KEY (accountId)
  REFERENCES public.accounts(id);

---

CREATE TABLE IF NOT EXISTS `multisigreceives` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,

  `accountId` BIGINT NOT NULL,
  `type` INT NOT NULL,
  `height` BIGINT NOT NULL,
  `transactionId` BIGINT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.multisigreceives ADD
  FOREIGN KEY (accountId)
  REFERENCES public.accounts(id);

---
--- indices
---

CREATE INDEX IDX_BLOCKS_TIMESTAMP ON `blocks` (timeStamp);
CREATE INDEX IDX_BLOCKS_HEIGHT ON `blocks` (height);
CREATE INDEX IDX_BLOCKS_HARVESTERID ON `blocks` (harvesterId);
CREATE INDEX IDX_BLOCKS_HARVESTEDINNAME ON `blocks` (harvestedInName);
CREATE INDEX IDX_BLOCKS_HARVESTERID_HEIGHT ON `blocks` (harvesterId, height desc);
CREATE INDEX IDX_BLOCKS_HARVESTEDINNAME_HEIGHT ON `blocks` (harvestedInName, height desc);

CREATE INDEX IDX_TRANSFERS_BLOCKID_ASC ON `transfers` (blockId ASC);
CREATE INDEX IDX_TRANSFERS_TIMESTAMP ON `transfers` (timeStamp);
CREATE INDEX IDX_TRANSFERS_SENDERID ON `transfers` (senderId);
CREATE INDEX IDX_TRANSFERS_RECIPIENTID ON `transfers` (recipientId);
CREATE INDEX IDX_TRANSFERS_SENDERID_ID ON `transfers` (senderId, id DESC);
CREATE INDEX IDX_TRANSFERS_RECIPIENTID_ID ON `transfers` (recipientId, id DESC);

CREATE INDEX IDX_IMPORTANCETRANSFERS_BLOCKID_ASC ON `importancetransfers` (blockId ASC);
CREATE INDEX IDX_IMPORTANCETRANSFERS_TIMESTAMP ON `importancetransfers` (timeStamp);
CREATE INDEX IDX_IMPORTANCETRANSFERS_SENDERID ON `importancetransfers` (senderId);
CREATE INDEX IDX_IMPORTANCETRANSFERS_REMOTEID ON `importancetransfers` (remoteId);
CREATE INDEX IDX_IMPORTANCETRANSFERS_SENDERID_ID ON `importancetransfers` (senderId, id DESC);
CREATE INDEX IDX_IMPORTANCETRANSFERS_REMOTEID_ID ON `importancetransfers` (remoteId, id DESC);

CREATE INDEX IDX_MULTISIGSIGNERMODIFICATIONS_TIMESTAMP ON `multisigsignermodifications` (timeStamp);
CREATE INDEX IDX_MULTISIGSIGNERMODIFICATIONS_SENDERID ON `multisigsignermodifications` (senderId);
CREATE INDEX IDX_MULTISIGSIGNERMODIFICATIONS_SENDERID_ID ON `multisigsignermodifications` (senderId, id DESC);

CREATE INDEX IDX_MULTISIGMODIFICATIONS_COSIGNATORYID ON `multisigmodifications` (cosignatoryId);
CREATE INDEX IDX_MULTISIGMODIFICATIONS_MULTISIGSIGNERMODIFICATIONID ON `multisigmodifications` (MultisigSignerModificationId DESC);

CREATE INDEX IDX_MULTISIGTRANSACTIONS_BLOCKID_ASC ON `multisigtransactions` (blockId ASC);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_TIMESTAMP ON `multisigtransactions` (timeStamp);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_SENDERID ON `multisigtransactions` (senderId);
CREATE INDEX IDX_MULTISIGTRANSACTIONS_SENDERID_ID ON `multisigtransactions` (senderId, id DESC);

CREATE INDEX IDX_MULTISIGSIGNATURES_TIMESTAMP ON `multisigsignatures` (timeStamp);
CREATE INDEX IDX_MULTISIGSIGNATURES_SENDERID ON `multisigsignatures` (senderId);
CREATE INDEX IDX_MULTISIGSIGNATURES_SENDERID_ID ON `multisigsignatures` (senderId, id DESC);

CREATE Unique INDEX IDX_MULTISIGSENDS_ACCOUNTID_TYPE_TRANSACTIONID ON `multisigsends` (accountId asc, type asc, transactionId DESC);

CREATE Unique INDEX IDX_MULTISIGRECEIVES_ACCOUNTID_TYPE_TRANSACTIONID ON `multisigreceives` (accountId asc, type asc, transactionId DESC);
