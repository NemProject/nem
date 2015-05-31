CREATE TABLE IF NOT EXISTS `mincosignatoriesmodifications` (
  `multisigSignerModificationId` BIGINT NOT NULL,

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `relativeChange` INT NOT NULL, -- the relative change of min cosignatories

  PRIMARY KEY(`id`)
);

ALTER TABLE public.mincosignatoriesmodifications ADD
  FOREIGN KEY (multisigSignerModificationId)
  REFERENCES public.multisigsignermodifications(id);
