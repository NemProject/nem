ALTER TABLE transfers ADD COLUMN `messageType` INT;
ALTER TABLE transfers ADD COLUMN `messagePayload` VARBINARY(1026);

