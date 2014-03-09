--DROP TABLE nis.`block_transfers`;
--DROP TABLE nis.`blocks`;
--DROP TABLE nis.`transfers`;
--DROP TABLE nis.`accounts`;

DROP DATABASE nis;

CREATE DATABASE `nis`;

GRANT CREATE, ALTER, INDEX, INSERT, SELECT, UPDATE, DELETE, DROP ON `nis`.* to 'nisuser'@'localhost' identified by 'nispass';
