CREATE TABLE IF NOT EXISTS `mincosignatoriesmodifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `relativeChange` INT NOT NULL, -- the relative change of min cosignatories

  PRIMARY KEY(`id`)
);

ALTER TABLE public.multisigsignermodifications ADD
  `mincosignatoriesmodificationId` BIGINT;

ALTER TABLE public.multisigsignermodifications ADD
  FOREIGN KEY (mincosignatoriesmodificationId)
  REFERENCES public.mincosignatoriesmodifications(id);
