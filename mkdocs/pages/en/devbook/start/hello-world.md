---
title: Hello World
tutorial_level: beginner
---

# Hello World

This tutorial shows how to verify that your Symbol SDK installation is working correctly by writing a minimal program
that:

* Retrieves the network name and launch date using the SDK.
* Connects to a <node:> and prints the current blockchain height.

No accounts, keys, or transactions are required, just a basic SDK call and a REST request.

## Prerequisites

If you have not done so already, start with [Setting Up a Development Environment](../start/setup.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/start/hello_world', ['py', 'js']) }}

### Making SDK Calls

{{ tutorial.code_snippet_tagged('step-1') }}

The <dy:NemFacade> class is the main entry point to the Symbol SDK when working with the NEM blockchain.
It provides most of the methods you will need, from building and signing transactions to retrieving network-related
information.

To create a facade, simply specify the name of the network you want to work with, either `mainnet` or `testnet`.

This example then demonstrates how to retrieve the network launch date.
The <dy:NetworkTimestampDatetimeConverter.toDatetime> method converts a network timestamp into a UTC datetime.
By passing `0` (the genesis timestamp) you can obtain the moment the genesis block was produced, that is, the network's
launch date.

### Retrieving Information From a Node

{{ tutorial.code_snippet_tagged('step-2') }}

Interaction with the NEM blockchain happens through a <node:>, which exposes a REST interface for querying network
state and submitting transactions.

This example connects to a testnet node and retrieves the current blockchain height from the <get:/chain/height>
endpoint.

This request does not require any private keys or authorization, making it a simple and effective test to confirm that
the environment is set up correctly and can reach the network.

## Output

The output shown below corresponds to a typical run of the program.

```text
--8<-- 'devbook/start/hello_world.log'
```

## Conclusion

If you got the output shown above, you're all set!
You have access to the Symbol SDK and successfully reached a NEM node.

That's all you need to start your NEM adventure.

Why not try [creating an account next](../accounts/create-from-private-key.md)?
