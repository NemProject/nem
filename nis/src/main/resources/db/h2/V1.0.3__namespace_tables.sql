CREATE TABLE IF NOT EXISTS `namespaces` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fullName` VARCHAR(148) NOT NULL, -- 16 + 64 + 64 + 2 + 2
  `ownerId` BIGINT NOT NULL,
  `height` BIGINT NOT NULL,
  `level` INT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.namespaces ADD
  FOREIGN KEY (ownerId)
  REFERENCES public.accounts(id);

CREATE TABLE IF NOT EXISTS `namespaceprovisions` (
  `blockId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL DEFAULT transaction_id_seq.nextval,
  `transferHash` VARBINARY(34) NOT NULL,

  `version` INT NOT NULL,
  `fee` BIGINT NOT NULL,
  `timestamp` INT NOT NULL,
  `deadline` INT NOT NULL,

  `senderId` BIGINT NOT NULL, -- reference to accounts
  `senderProof` VARBINARY(66), -- can be null for multisig TXes
  `rentalFeeSinkId` BIGINT NOT NULL, -- reference to accounts
  `rentalFee` BIGINT NOT NULL,
  `namespaceId` BIGINT NOT NULL, -- reference to namespaces

  `blkIndex` INT NOT NULL, -- index inside block
  `referencedTransaction` BIGINT NOT NULL,

  PRIMARY KEY (`id`)
);

ALTER TABLE public.namespaceprovisions ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

ALTER TABLE public.namespaceprovisions ADD
  FOREIGN KEY (senderId)
  REFERENCES public.accounts(id);

ALTER TABLE public.namespaceprovisions ADD
  FOREIGN KEY (rentalFeeSinkId)
  REFERENCES public.accounts(id);

ALTER TABLE public.namespaceprovisions ADD
  FOREIGN KEY (namespaceId)
  REFERENCES public.namespaces(id);

ALTER TABLE public.multisigtransactions ADD
  COLUMN `namespaceProvisionId` BIGINT;

ALTER TABLE public.multisigtransactions ADD
  FOREIGN KEY (namespaceProvisionId)
  REFERENCES public.namespaceprovisions(id);

CREATE INDEX IDX_NAMESPACES_FULLNAME_ASC ON `namespaces` (fullName ASC);
CREATE INDEX IDX_NAMESPACES_OWNERID ON `namespaces` (ownerId);
CREATE INDEX IDX_NAMESPACES_OWNERID_ID ON `namespaces` (ownerId, id DESC);
CREATE INDEX IDX_NAMESPACES_HEIGHT ON `namespaces` (height ASC);
CREATE INDEX IDX_NAMESPACES_LEVEL ON `namespaces` (level ASC);

CREATE INDEX IDX_NAMESPACEPROVISIONS_BLOCKID_ASC ON `namespaceprovisions` (blockId ASC);
CREATE INDEX IDX_NAMESPACEPROVISIONS_TIMESTAMP ON `namespaceprovisions` (timeStamp);
CREATE INDEX IDX_NAMESPACEPROVISIONS_SENDERID ON `namespaceprovisions` (senderId);
CREATE INDEX IDX_NAMESPACEPROVISIONS_SENDERID_ID ON `namespaceprovisions` (senderId, id DESC);
CREATE INDEX IDX_NAMESPACEPROVISIONS_RENTALFEESINKID ON `namespaceprovisions` (rentalFeeSinkId);
CREATE INDEX IDX_NAMESPACEPROVISIONS_RENTALFEESINKID_ID ON `namespaceprovisions` (rentalFeeSinkId, id DESC);
