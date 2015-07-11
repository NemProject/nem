CREATE TABLE IF NOT EXISTS `mosaics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `creatorId` BIGINT NOT NULL, -- reference to accounts
  `name` VARCHAR(34) NOT NULL,
  `description` VARCHAR(514) NOT NULL,
  `namespaceId` VARCHAR(148) NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaics ADD
  FOREIGN KEY (creatorId)
  REFERENCES public.accounts(id);

CREATE TABLE IF NOT EXISTS `mosaicproperties` (
  `mosaicId` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(34) NOT NULL,
  `value` VARCHAR(34) NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaicproperties ADD
  FOREIGN KEY (mosaicId)
  REFERENCES public.mosaics(id);

CREATE TABLE IF NOT EXISTS `mosaiccreationtransactions` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `mosaicId` BIGINT NOT NULL, -- reference to mosaics

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaiccreationtransactions ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.mosaiccreationtransactions ADD
  FOREIGN KEY (senderId)
  REFERENCES public.accounts(id);

ALTER TABLE public.mosaiccreationtransactions ADD
  FOREIGN KEY (mosaicId)
  REFERENCES public.mosaics(id);

CREATE TABLE IF NOT EXISTS `smarttilesupplychanges` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `mosaicId` BIGINT NOT NULL,
  `supplyType` INT NOT NULL,
  `quantity` BIGINT NOT NULL,

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL, -- do we want this?

  PRIMARY KEY (`id`)
);

ALTER TABLE public.smarttilesupplychanges ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.smarttilesupplychanges ADD
  FOREIGN KEY (senderId)
  REFERENCES public.accounts(id);

ALTER TABLE public.multisigtransactions ADD
  COLUMN `mosaicCreationId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (mosaicCreationId)
  REFERENCES public.mosaicCreationTransactions(id);

ALTER TABLE public.multisigtransactions ADD
  COLUMN `smartTileSupplyChangeId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (smartTileSupplyChangeId)
  REFERENCES public.smartTileSupplyChanges(id);

CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_BLOCKID_ASC ON `mosaiccreationtransactions` (blockId ASC);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_TIMESTAMP ON `mosaiccreationtransactions` (timeStamp);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_MOSAICID ON `mosaiccreationtransactions` (mosaicId);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_SENDERID ON `mosaiccreationtransactions` (senderId);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_SENDERID_ID ON `mosaiccreationtransactions` (senderId, id DESC);

CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_BLOCKID_ASC ON `smarttilesupplychanges` (blockId ASC);
CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_TIMESTAMP ON `smarttilesupplychanges` (timeStamp);
CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_SUPPLYTYPE ON `smarttilesupplychanges` (supplyType);
CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_MOSAICID ON `smarttilesupplychanges` (mosaicId);
CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_SENDERID ON `smarttilesupplychanges` (senderId);
CREATE INDEX IDX_SMARTTILESUPPLYCHANGES_SENDERID_ID ON `smarttilesupplychanges` (senderId, id DESC);

CREATE INDEX IDX_MOSAICS_NAME ON `mosaics` (name);
CREATE INDEX IDX_MOSAICS_NAMESPACEID ON `mosaics` (namespaceId);
CREATE INDEX IDX_MOSAICS_NAMESPACEID_NAME ON `mosaics` (namespaceId, name);
CREATE INDEX IDX_MOSAICS_CREATORID ON `mosaics` (creatorId);
CREATE INDEX IDX_MOSAICS_CREATORID_ID ON `mosaics` (creatorId, id DESC);

CREATE INDEX IDX_MOSAICPROPERTIES_MOSAICID ON `mosaicproperties` (mosaicId);
