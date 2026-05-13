# Consensus

Consensus
:   The process by which all <nodes:> in the network agree on the current state of the blockchain.

With consensus, the network preserves a single, consistent timeline of <blocks:> and their <transactions:>,
and therefore the balance and data associated with every <account:>.

Consensus provides two forms of agreement:

* **Sealing agreement**:
    each block correctly links to its predecessor, ensuring the immutability of the chain's history.

* **Content agreement**:
    all transactions in a block comply with the network's rules.
    For example, transferring tokens from an account requires a valid signature from its <private key:>
    and a sufficient balance.

Blocks that violate either form of agreement are _invalid_ and ignored by well-behaved nodes.
Such blocks are not propagated through the network.

## Conflicts

In a decentralized network like NEM, <nodes:> may temporarily become disconnected.
This can happen due to latency, connectivity issues, or changes in network topology.

During _network partitions_, disconnected groups of nodes may temporarily disagree on the most recent blocks,
even though they might all be valid.

As a result, more than one version of the blockchain may exist for a short time, a situation known as a _fork_.

Fork
:   State where two or more competing chains share a common history but differ in their latest blocks.

During a fork, for example, queries to different nodes might return different balances for the same account,
depending on whether the queried nodes have seen all the transactions that affect that account.

When connectivity is restored, nodes might encounter competing blocks for the same height, resulting in a conflict.

Forks might also occur naturally when two nodes produce a new block at the same time.

## Conflict Resolution

When a node becomes aware of a fork, NEM resolves it using a deterministic rule:
the chain with the highest <chain score:> is considered the correct one.

Nodes on the lower-scoring fork need to _roll back_ any blocks that are no longer part of the
main chain and switch to the better one.

Rollback
:   The process of discarding one or more recently added blocks when a node switches to a better chain,
    typically after a fork is resolved.

Any transactions in the discarded blocks that are not already present in the main chain
return to the <unconfirmed pool:> and must be re-verified before they can be included in a block again.

Rollbacks on NEM are usually shallow and rare, affecting only the most recent blocks.

Rewrite limit
:   The maximum depth a rollback can reach on NEM, set to **360 blocks** (approximately six hours).

Each new block adds another layer of confirmation, and once a transaction sits past the rewrite limit, it is
considered irreversible.
