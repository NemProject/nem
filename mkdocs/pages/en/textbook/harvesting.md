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

* Holds at least 10'000 <XEM:> in <vesting:|vested> balance.
* Is connected to a <node:>, either directly or through delegation.

The account's <importance:> score determines how often it can harvest.

## Harvesting Process

NEM has no central coordinator to determine which node will harvest the next block.
Instead, every <node:> independently competes by running the same deterministic eligibility check with each of its
<harvester accounts:>.

To do this, a _target_ value is calculated based primarily on each account's <importance:>.
The higher the importance, the higher its target will be.

For each of its harvester accounts, the node computes a number called the _hit_ from the candidate block's
[generation hash](./blocks.md#derived-fields).

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

Node owners can participate in harvesting by enabling [local](#local-harvesting) or [remote](#remote-harvesting)
harvesting, depending on their preferred balance between simplicity and security.
Accounts that do not operate a node but meet the balance requirements can still harvest by linking to a node through
[delegated harvesting](#delegated-harvesting).

### Local Harvesting

Local Harvesting
:   A type of <harvesting:> where the rewards are sent directly to the harvester account.
    The <node:> signs produced <blocks:> using the operator's <main key:>, which must be stored on the machine.

!!! warning
    The harvester account must hold a significant balance to maintain a high <importance:> score.
    Storing its private key on a machine that is permanently online puts the entire balance at risk
    in case of unauthorized access.

While local harvesting offers a straightforward setup, these security risks make it unsuitable for public nodes.
Most operators instead prefer remote harvesting.

### Remote Harvesting

Remote Harvesting
:   A type of <harvesting:> that delegates block signing to a separate <remote key:|remote account>, while the node's
    <importance:> score and rewards remain tied to the operator's <main key:>.

The remote account holds no funds and exists only to sign blocks on behalf of the harvester's main account.
Because its <private key:> is stored in the node's configuration files, hosted on a permanently-online machine, it is
designed to be expendable.

The remote account is designated by signing an _Account Key Link_ transaction, which transfers the main account's
<importance:> to it.
The remote account begins signing blocks after a settling period of 360 blocks (approximately six hours), and a second
_Account Key Link_ transaction removes it, subject to the same delay.

The main account still determines the node's importance and receives all block rewards.
However, its key remains offline, safe from compromise.
For simplicity, the main account is still called the harvester account, even though blocks are signed by the remote
account.

This separation of duties offers strong protection for the harvester's funds and makes remote harvesting the preferred
option for most operators.

### Delegated Harvesting

Delegated Harvesting
:   A form of <harvesting:> that lets an eligible account that does not operate a node delegate harvesting duties to a
    third-party node.
    The delegating account's <importance:> score is used, and it receives the harvested rewards in full.

Such an account is called a _delegator_, or _delegated harvester_.

Delegator
:   An account that <delegated harvesting:|delegates> harvesting to a third-party node while retaining its <importance:>
    and receiving the harvested rewards.
    Also called a _delegated harvester_.

Although the node performs the work, the delegator is still considered the harvester, and NEM pays it the block rewards
in full.
The arrangement lets an account earn rewards without running a node of its own.

Delegated harvesting uses the same remote account setup as remote harvesting.
The delegator provides the remote account's <private key:> to the third-party node, which adds the account to the set it
harvests for and signs blocks on the delegator's behalf.

Whether the node accepts the remote account depends on the operator's policy, and the delegator can revoke the
arrangement at any time.

As with remote harvesting, block signing is performed by an account other than the delegator, so its <private key:>
never needs to leave secure storage.

## Reward Distribution

When a <block:> is harvested, the harvester receives the sum of fees from every <transaction:> in the block.

With <local harvesting:>, the harvester signs its own blocks and receives the rewards directly.
With <remote harvesting:> and <delegated harvesting:>, the remote account signs the blocks, but the rewards still
flow to the main account, never to the remote account or to the node operator hosting it.

NEM does not split block rewards between the harvester and the node operator.
A node that hosts a remote account for someone else receives nothing from those blocks.
The protocol pays the harvester in full.
