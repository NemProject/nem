DELETE FROM transfers;
DELETE FROM block_transfers;
DELETE FROM blocks;
DELETE FROM accounts;
--ALTER TABLE blocks ADD COLUMN `generationHash` VARBINARY(34);
--UPDATE blocks SET `generationHash`=0xc5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8 WHERE `generationHash` is NULL;
--ALTER TABLE blocks ALTER COLUMN `generationHash` VARBINARY(34) NOT NULL;


ALTER TABLE blocks ADD COLUMN `generationHash` VARBINARY(34);
