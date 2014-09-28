ALTER TABLE `blocks` ADD COLUMN harvestedInName BIGINT;

ALTER TABLE public.blocks ADD
  FOREIGN KEY (harvestedInName)
  REFERENCES public.accounts(id);

