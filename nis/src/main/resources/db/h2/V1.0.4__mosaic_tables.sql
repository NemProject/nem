CREATE TABLE IF NOT EXISTS `mosaicdefinitions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `creatorId` BIGINT NOT NULL, -- reference to accounts
  `name` VARCHAR(34) NOT NULL,
  `description` VARCHAR(514) NOT NULL,
  `namespaceId` VARCHAR(148) NOT NULL,
  `feeType` INT,
  `feeRecipientId` BIGINT, -- reference to accounts
  `feeDbMosaicId` BIGINT,
  `feeQuantity` BIGINT,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaicdefinitions ADD
  FOREIGN KEY (creatorId)
  REFERENCES public.accounts(id);

ALTER TABLE public.mosaicdefinitions ADD
  FOREIGN KEY (feeRecipientId)
  REFERENCES public.accounts(id);

CREATE TABLE IF NOT EXISTS `mosaicproperties` (
  `mosaicDefinitionId` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(34) NOT NULL,
  `value` VARCHAR(34) NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaicproperties ADD
  FOREIGN KEY (mosaicDefinitionId)
  REFERENCES public.mosaicdefinitions(id);

CREATE TABLE IF NOT EXISTS `mosaicdefinitioncreationtransactions` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `mosaicDefinitionId` BIGINT NOT NULL, -- reference to mosaicdefinitions
  `creationFeeSinkId` BIGINT NOT NULL, -- reference to accounts
  `creationFee` BIGINT NOT NULL,

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaicdefinitioncreationtransactions ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.mosaicdefinitioncreationtransactions ADD
  FOREIGN KEY (senderId)
  REFERENCES public.accounts(id);

ALTER TABLE public.mosaicdefinitioncreationtransactions ADD
  FOREIGN KEY (mosaicDefinitionId)
  REFERENCES public.mosaicDefinitions(id);

ALTER TABLE public.mosaicdefinitioncreationtransactions ADD
  FOREIGN KEY (creationFeeSinkId)
  REFERENCES public.accounts(id);

CREATE TABLE IF NOT EXISTS `mosaicsupplychanges` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `dbMosaicId` BIGINT NOT NULL,
  `supplyType` INT NOT NULL,
  `quantity` BIGINT NOT NULL,

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaicsupplychanges ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.mosaicsupplychanges ADD
  FOREIGN KEY (senderId)
  REFERENCES public.accounts(id);

CREATE TABLE IF NOT EXISTS `transferredmosaics` (
  `transferId` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dbMosaicId` BIGINT NOT NULL,
  `quantity` BIGINT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.transferredmosaics  ADD
  FOREIGN KEY (transferId)
  REFERENCES public.transfers(id);

ALTER TABLE public.multisigtransactions ADD
  COLUMN `mosaicDefinitionCreationId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (mosaicDefinitionCreationId)
  REFERENCES public.mosaicDefinitionCreationTransactions(id);

ALTER TABLE public.multisigtransactions ADD
  COLUMN `mosaicSupplyChangeId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (mosaicSupplyChangeId)
  REFERENCES public.mosaicSupplyChanges(id);

CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_BLOCKID_ASC ON `mosaicdefinitioncreationtransactions` (blockId ASC);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_TIMESTAMP ON `mosaicdefinitioncreationtransactions` (timeStamp);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_MOSAICDEFINITIONID ON `mosaicdefinitioncreationtransactions` (mosaicDefinitionId);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_SENDERID ON `mosaicdefinitioncreationtransactions` (senderId);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_SENDERID_ID ON `mosaicdefinitioncreationtransactions` (senderId, id DESC);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_CREATIONFEESINKID ON `mosaicdefinitioncreationtransactions` (creationFeeSinkId);
CREATE INDEX IDX_MOSAICDEFINITIONCREATIONTRANSACTIONS_CREATIONFEESINKID_ID ON `mosaicdefinitioncreationtransactions` (creationFeeSinkId, id DESC);

CREATE INDEX IDX_MOSAICSUPPLYCHANGES_BLOCKID_ASC ON `mosaicsupplychanges` (blockId ASC);
CREATE INDEX IDX_MOSAICSUPPLYCHANGES_TIMESTAMP ON `mosaicsupplychanges` (timeStamp);
CREATE INDEX IDX_MOSAICSUPPLYCHANGES_SUPPLYTYPE ON `mosaicsupplychanges` (supplyType);
CREATE INDEX IDX_MOSAICSUPPLYCHANGES_DBMOSAICID ON `mosaicsupplychanges` (dbMosaicId);
CREATE INDEX IDX_MOSAICSUPPLYCHANGES_SENDERID ON `mosaicsupplychanges` (senderId);
CREATE INDEX IDX_MOSAICSUPPLYCHANGES_SENDERID_ID ON `mosaicsupplychanges` (senderId, id DESC);

CREATE INDEX IDX_MOSAICDEFINITIONS_NAME ON `mosaicdefinitions` (name);
CREATE INDEX IDX_MOSAICDEFINITIONS_NAMESPACEID ON `mosaicdefinitions` (namespaceId);
CREATE INDEX IDX_MOSAICDEFINITIONS_NAMESPACEID_NAME ON `mosaicdefinitions` (namespaceId, name);
CREATE INDEX IDX_MOSAICDEFINITIONS_CREATORID ON `mosaicdefinitions` (creatorId);
CREATE INDEX IDX_MOSAICDEFINITIONS_CREATORID_ID ON `mosaicdefinitions` (creatorId, id DESC);
CREATE INDEX IDX_MOSAICDEFINITIONS_RECIPIENTID ON `mosaicdefinitions` (feeRecipientId);
CREATE INDEX IDX_MOSAICDEFINITIONS_FEERECIPIENTID_ID ON `mosaicdefinitions` (feeRecipientId, id DESC);
CREATE INDEX IDX_MOSAICDEFINITIONS_FEETYPE ON `mosaicdefinitions` (feeType);
CREATE INDEX IDX_MOSAICDEFINITIONS_FEEDBMOSAICID ON `mosaicdefinitions` (feeDbMosaicId);
CREATE INDEX IDX_MOSAICDEFINITIONS_FEEQUANTITY ON `mosaicdefinitions` (feeQuantity);

CREATE INDEX IDX_MOSAICPROPERTIES_MOSAICDEFINITIONID ON `mosaicproperties` (mosaicDefinitionId);

CREATE INDEX IDX_TRANSFERREDMOSAICS_DBMOSAICID ON `transferredmosaics` (dbMosaicId ASC);
