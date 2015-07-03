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

CREATE TABLE IF NOT EXISTS `mosaics` (
  `mosaicCreationTransactionId` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `creatorId` BIGINT NOT NULL, -- reference to accounts
  `mosaicId` VARCHAR(34) NOT NULL,
  `description` VARCHAR(514) NOT NULL,
  `namespaceId` VARCHAR(148) NOT NULL,
  `amount` BIGINT NOT NULL,
  `position` INTEGER NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.mosaics ADD
  FOREIGN KEY (mosaicCreationTransactionId)
  REFERENCES public.mosaicCreationTransactions(id);

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

ALTER TABLE public.multisigtransactions ADD
  COLUMN `mosaicCreationId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (mosaicCreationId)
  REFERENCES public.mosaicCreationTransactions(id);

CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_BLOCKID_ASC ON `mosaiccreationtransactions` (blockId ASC);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_TIMESTAMP ON `mosaiccreationtransactions` (timeStamp);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_SENDERID ON `mosaiccreationtransactions` (senderId);
CREATE INDEX IDX_MOSAICCREATIONTRANSACTIONS_SENDERID_ID ON `mosaiccreationtransactions` (senderId, id DESC);

CREATE INDEX IDX_MOSAICS_MOSAICCREATIONTRANSACTIONID ON `mosaics` (mosaicCreationTransactionId);
CREATE INDEX IDX_MOSAICS_MOSAICID ON `mosaics` (mosaicId);
CREATE INDEX IDX_MOSAICS_NAMESPACEID ON `mosaics` (namespaceId);
CREATE INDEX IDX_MOSAICS_NAMESPACEID_MOSAICID ON `mosaics` (namespaceId, mosaicId);
CREATE INDEX IDX_MOSAICS_CREATORID ON `mosaics` (creatorId);
CREATE INDEX IDX_MOSAICS_CREATORID_ID ON `mosaics` (creatorId, id DESC);

CREATE INDEX IDX_MOSAICPROPERTIES_MOSAICID ON `mosaicproperties` (mosaicId);
