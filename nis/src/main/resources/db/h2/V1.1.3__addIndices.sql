CREATE INDEX IDX_TRANSFERS_SENDERID_TIMESTAMP_ID ON `transfers` (senderId, timestamp desc, id desc);
CREATE INDEX IDX_TRANSFERS_RECIPIENTID_TIMESTAMP_ID ON `transfers` (recipientId, timestamp desc, id desc);
