# Harvesting

Harvesting
:   The process by which NEM adds new <blocks:> to the chain and distributes rewards to participating <accounts:>.
    It plays a similar role to **mining** in <PoW:> or **staking** in <PoS:>.

Each new block is produced by a single <node:> on behalf of one of its <harvester accounts:>.
A node's chance of producing the next block is weighted by the combined <importance:> of its harvester accounts.

Harvester account
:   An account participating in harvesting.
    Its importance determines its chance of producing blocks, and it receives the rewards from each block it harvests.

The fees from the <transactions:> included in the block are paid in full to a single harvester account, the one whose
importance backed the block.

## Eligibility

Unlike mining in <PoW:>, harvesting does not require specialized hardware.

Participation in harvesting is open to any <account:> that:

* Holds at least 10'000 <XEM:> in <vesting:|vested balance>.
* Is connected to a <node:>, either directly or through delegation.

The account's <importance:> score determines how often it can harvest.

## Harvesting Process

NEM has no central coordinator to determine which node will harvest the next block.
Instead, every <node:> independently competes by running the same deterministic eligibility check with each of its
<harvester accounts:>.

To do this, a _target_ value is calculated based primarily on each account's <importance:>.
The higher the importance, the higher its target will be.

For each of its harvester accounts, the node computes a number called the _hit_ from the candidate block's
[generation hash](blocks.md#derived-fields).

If any of its harvester accounts produces a hit below the target, the node assembles a candidate block
from the <unconfirmed pool:> and announces it to the rest of the network.

Other nodes then verify the block, ensuring:

* The block signature comes from the claimed harvester.
* The <transactions:> are valid.
* The hit is indeed lower than the target.

If any of these checks fail, other nodes simply ignore the new block.
The <consensus:> mechanism makes sure that the node eventually adopts a block that the rest of the network agrees on.

If the block is valid, it is accepted by other nodes that include it in their copies of the chain.
The cycle repeats at the next block height.

!!! info "Simultaneous Block Creation"
    Note that no special measures are in place to prevent multiple nodes from generating blocks at the same height.
    When this occurs, the network may temporarily <fork:> as different nodes adopt different blocks for the same
    position in the chain.

    The <consensus:> mechanism resolves these conflicts as nodes become aware of the competing blocks.

??? abstract "Target and Hit Calculation"

    * The **target** is calculated independently by each node and reflects the likelihood of harvesting the next block
        using a specific account.
        It depends on three factors:

        * The account's <importance:> score: more active or better-funded accounts will harvest more often.
        * The network-wide **difficulty**, which adjusts dynamically based on recent block production times,
            to maintain a constant rate.
        * The **time elapsed** since the last block: longer delays increase the chance of a new block being produced.

    * The **hit** is derived deterministically from the block's [generation hash](blocks.md#derived-fields), which is
        itself the hash of the previous block's generation hash combined with the harvester's <public key:>.
        The hit therefore depends on the full chain of past harvesters and on who is attempting to
        harvest now.

    For the block to be valid, the node's target must be **greater than** its hit.
    A higher importance or a longer delay increases the target, while a higher difficulty decreases it.

## Harvesting Methods

NEM supports two <harvesting:> modes, [local](#local-harvesting) and [delegated](#delegated-harvesting), which differ in
how the signing key is held.

### Local Harvesting

Local Harvesting
:   A type of harvesting where the <node:> signs produced <blocks:> using the harvester's <private key:>,
    which must be stored on the machine.

!!! warning
    The harvester account must hold a significant balance to maintain a high <importance:> score.
    Storing its private key on a machine that is permanently online puts the entire balance at risk
    in case of unauthorized access.

While local harvesting offers a straightforward setup, these security risks make it unsuitable for public nodes.
Most operators instead prefer <delegated harvesting:>.

### Delegated Harvesting

Delegated Harvesting
:   A form of harvesting where an account delegates block-signing to a separate <remote account:> on a node,
    while keeping its importance and receiving all the rewards.

Delegated harvesting involves two accounts, the _lessor_ and the _remote account_:

Lessor
:   An account that delegates its harvesting rights to a remote account, retaining its importance and
    receiving all fees from blocks harvested on its behalf. Also called the _delegated harvester_.

Remote account
:   An account dedicated to signing blocks on a lessor's behalf, with its private key kept on the node.

Delegated harvesting is activated by signing an _importance transfer transaction_ that designates a remote
account to sign on the lessor's behalf.
After a settling period of 360 blocks (approximately six hours), the remote account begins signing blocks on the
lessor's behalf.

The protocol enforces that a remote account holds no funds: it must have zero balance at activation, and afterwards
cannot send or receive transfers.
Stealing the remote key therefore grants no access to the lessor's funds, which remain controlled by the lessor's
private key (which can stay offline).

A stolen remote key could still be used to harvest blocks on the lessor's behalf from another node, but block rewards
continue to flow to the lessor's account regardless of who controls the remote key.
The lessor can deactivate the remote by announcing another importance transfer transaction.
The deactivation takes effect after the same 360-block settling period, at which point the remote stops harvesting.

This separation of duties makes delegated harvesting the preferred option for most operators and for any account that
does not run its own node.

## Reward Distribution

When a <block:> is harvested, the harvester receives the sum of fees from every <transaction:> in the block.

In <delegated harvesting:>, the rewards go to the <lessor:>, not to the <remote account:> that signed the block or to
the node operator's own account.
In <local harvesting:> the harvester is the signer and receives the rewards directly.

NEM does not split block rewards between the harvester and the node operator.
A node hosting a lessor's remote account receives nothing from those blocks.
The protocol pays the lessor in full.

A node operator can still earn block rewards by harvesting with their own account, either locally or by delegating it to
a remote on their own node (using the same security pattern as any other lessor).
For hosting other lessors, incentives come from external programs rather than from block rewards.

!!! note "Supernode Program"
    Because NEM has no block subsidy or inflation, transaction fees alone are typically modest.
    To sustain high-quality public node infrastructure, the Symbol Syndicate runs the **Supernode Program**, which pays
    nodes that pass daily availability, performance, and connectivity checks.

    Rewards come from a fund set aside in the nemesis block for this purpose.

    The program runs as a separate service, not as part of the consensus protocol.

## Importance

Importance
:   A measure of an <account:>'s contribution to the network, based on its vested balance and its outgoing transfers to
    other accounts.
    This score determines the account's chances of harvesting a <block:>.

Importance serves a role similar to hashrate in <PoW:> systems or stake in <PoS:> systems:
the higher the value, the greater the chance to harvest a block and earn rewards.

### Vesting

Vesting
:   The process by which an account's <XEM:> balance gradually matures from _unvested_ to _vested_.
    Only the vested portion counts toward the account's importance, so newly funded accounts do not
    start harvesting immediately.

When an account first receives XEM, the full amount is unvested.
Every 1440 blocks (about one day at the 60-second target), 10% of the unvested balance migrates to the vested portion.
The same step repeats each day, gradually moving more of the balance to vested.

For example:

* After day 1, 10% of the original balance is vested.
* After day 2, 19% is vested.
* After day 7, just over half is vested.
* The balance asymptotically approaches fully vested.

Larger holdings cross the 10'000 XEM vested threshold sooner.
An account holding 100'000 XEM, for instance, vests 10'000 XEM at its first vesting cycle (after about one day)
and becomes harvesting-eligible at that point.

??? info "Importance Calculation"

    All accounts that have at least 10'000 XEM in vested balance are eligible to harvest and participate in the
    importance calculation.

    The importance score for an eligible account combines:

    * Its **vested balance**.
    * A **PageRank-like score** computed over the graph of outgoing transfer transactions.

    Only transfers that meet both of the following are considered:

    * The transfer occurred within the last 43200 blocks (about 30 days).
    * The recipient is itself eligible (has at least 10'000 vested XEM).

    Each qualifying transfer contributes its amount, with older transfers counting for less (10% less per day).
    If two accounts sent XEM to each other, only the difference counts.
    That difference must be at least 1'000 XEM to contribute to the score.

    The full algorithm is the _Proof-of-Importance_ (PoI) scheme described in the
    [NEM Technical Reference](site:/assets/pdfs/NEM_techRef.pdf), section 7.

!!! note
    Importance scores are recalculated every 359 blocks (roughly 6 hours), and the recalculated value applies to all
    subsequent blocks until the next recalculation.
