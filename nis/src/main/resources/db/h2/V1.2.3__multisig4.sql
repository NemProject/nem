-- note: multisigsignature cannot be inside multisigTransaction
-- and multisigTransaction cannot be inside multisigTransaction,
-- so it's not needed to allow NULL in proofs in those columns

ALTER TABLE transfers ALTER COLUMN senderProof SET NULL;
ALTER TABLE importancetransfers ALTER COLUMN senderProof SET NULL;
ALTER TABLE multisigsignermodifications ALTER COLUMN senderProof SET NULL;

