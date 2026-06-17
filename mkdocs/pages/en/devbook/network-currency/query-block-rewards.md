---
title: Query Block Rewards
tutorial_level: beginner
---

# Querying Block Rewards

Each <block:> on NEM is produced by a single <harvester account:>.
The entire reward for harvesting a block comes from the <transaction:> fees collected in that block, and these fees are
paid in full to the harvester that produced it.

This tutorial shows how to query any block, identify its harvester, and sum the transaction fees that form the reward.

## Prerequisites

Before you start, [set up your development environment](../start/setup.md).

This tutorial only reads data from the network. No <account:> or <XEM:> balance is required.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/network-currency/query_block_rewards', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default <testnet:> node is used.

The `BLOCK_HEIGHT` environment variable selects which block to query.
If not set, it defaults to `661258`, a block harvested on testnet.

## Code Explanation

The code fetches a block by height and derives the harvester address from the block's signer public key.
It then sums the fees of every transaction in the block to obtain the total reward.

### Fetching Block Information

{{ tutorial.code_snippet_tagged('step-1') }}

The <post:/block/at/public> endpoint returns the block at the requested height.
The request body is a JSON object containing the target height.

The response includes the block's `signer`, which is the harvester's <public key:>, and its `transactions` array.

### Identifying the Harvester

{{ tutorial.code_snippet_tagged('step-2') }}

The `signer` field holds the public key of the account that harvested the block.
The <dy:network.publicKeyToAddress> method converts this public key into the corresponding testnet <address:>.

!!! info "The signer is not always the account that earns the reward"

    With local harvesting, the `signer` is the <harvester account:> itself, so the derived address is the account that
    earns the reward.

    With <delegated harvesting:>, the `signer` is a <remote account:>, so the derived address is that account, not the
    <lessor:> that actually earns the reward.

### Summing the Transaction Fees

{{ tutorial.code_snippet_tagged('step-3') }}

Each transaction in the block has a `fee` field expressed in atomic units.
XEM has a <divisibility:> of 6, so `350000` atomic units represent `0.350000` XEM.

Adding the fees of every transaction gives the total reward for the block.

### Calculating the Total Reward

{{ tutorial.code_snippet_tagged('step-4') }}

The total block reward equals the sum of all transaction fees, paid in full to the harvester.
An empty block has no fees, and therefore no reward.

!!! note "Listing every reward an account has earned"

    The <get:/account/harvests> endpoint lists the rewards an account has earned.
    It returns one entry per block the account harvested, each with a `totalFee` field holding the reward collected for
    that block.

    The endpoint takes the address of the account that earns the rewards: the <harvester account:> for local harvesting,
    or the <lessor:> for <delegated harvesting:>.
    A <remote account:> address also returns the blocks that remote signed on the lessor's behalf.

## Output

The following output shows a typical run querying the rewards for block 661,258:

```text linenums="1" hl_lines="4 7-8 10"
--8<-- 'devbook/network-currency/query_block_rewards.log'
```

Some highlights from the output:

* **Harvester** (line 4): The address derived from the block's `signer` public key.
    This block was harvested locally, so the signer is the harvester credited with the reward.
* **Transaction fees** (lines 7 to 8): The fee paid by each transaction included in the block.
* **Total block reward** (line 10): The sum of all transaction fees paid in full to the harvester.

## Conclusion

This tutorial showed how to:

| Step                                                      | Related documentation                      |
| --------------------------------------------------------- | ------------------------------------------ |
| [Fetch block information](#fetching-block-information)    | <post:/block/at/public>                    |
