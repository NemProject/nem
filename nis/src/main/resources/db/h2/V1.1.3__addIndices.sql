CREATE INDEX IDX_TRANSFERS_SENDERID_ID ON `transfers` (senderId, id desc);
CREATE INDEX IDX_TRANSFERS_RECIPIENTID_ID ON `transfers` (recipientId, id desc);
