# NEM

NEM is a movement centered around NEM crypto-currency. Basis for NEM is **first** Proof-of-Importance / Proof-of-Stake system.

As other crypto-coins, NEM secures transactions in the blockchain.

Blockchain is actually distributed transaction database, shared by all peers participating in NEM network.

[more Blockchain](Blockchain)

## Basics

### Addresses

To identify accounts, NEM uses addresses. Addresses are 40 characters long, consist of characters (A-Z, 2-7) and always begin with an **N**. (e.g. `NBERUJIKSAPW54YISFOJZ2PLG3E7CACCNN2Z6SOW`) or **T** in test network (`TBERUJIKSAPW54YISFOJZ2PLG3E7CACCNP3PP3P6`)

You can think of an address as of bank account number.

[more Addresses](Addresses)

### Confirmations and harvesting.

Every transaction is confirmed by the network. Multiple transactions are packed together in blocks.
Creating a block is called **harvesting**. In contrast with Proof-of-Work (PoW) coins like bitcoin, litecoin, etc., harvesting is **NOT calculation-heavy** process.

Every transaction have associated fee. **Harvester** is rewarded with the fees from a block.

### Zero-inflation

As in NXT there is no inflation in NEM. The rewards for *harvesters* come ONLY from fees.

## Security

### Signing transactions

Security of NEM relies on [public key cryptography](http://en.wikipedia.org/wiki/Public-key_cryptography).
More specifically on [Elliptic Curve Digital Signature Algorithm](https://en.wikipedia.org/wiki/Elliptic_Curve_DSA) (ECDSA).

The curve that NEM uses {secp256k1 | ed25519}

### Transaction Malleability

To deal with signature malleability NEM uses **canonical signatures**

### Secure addresses

NXT has been criticized many times, for it's "short" addresses, which are only 2^64 long.
Due to due to [Birthday attack](http://en.wikipedia.org/wiki/Birthday_attack)
probability of hitting random "unconfirmed" address is bigger than 50% with as few as **2^33** trials. 

As stated earlier NEM uses addresses which are much longer 40 characters (36 bytes).

## advanced topics

### Timestamp service

Every transaction can contain 1024-byte "message". This means, that you can easily timestamp your data and have verifiable PROOF.

Let's assume you have created document, that you don't want publish yet. You can calculate it's hash (i.e. SHA-3-512) and include the hash as a message in some transaction. Once the transaction will be included in the blockchain, you have a proof, that the hash - and therefore document itself - was created before timestamp of block including it.
