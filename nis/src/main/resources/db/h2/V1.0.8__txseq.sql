ALTER TABLE `transfers` ADD COLUMN orderId INT NOT NULL DEFAULT(0);
UPDATE transfers SET orderId = blkIndex;

ALTER TABLE `importancetransfers` ADD COLUMN orderId INT NOT NULL DEFAULT(0);
UPDATE importancetransfers SET orderId = blkIndex;

