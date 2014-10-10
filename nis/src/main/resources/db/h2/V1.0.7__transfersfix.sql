ALTER TABLE `transfers` ADD COLUMN blockId BIGINT NOT NULL DEFAULT(0);

-- h2 syntax
UPDATE transfers t SET blockId = (SELECT block_id FROM block_transfers bt WHERE t.id=bt.transfer_id);

ALTER TABLE public.transfers ADD
  FOREIGN KEY (blockId)
  REFERENCES public.blocks(id);

DROP TABLE public.block_transfers;