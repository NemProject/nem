---
title: Get Namespace Information
tutorial_level: beginner
---

# Getting Namespace Information

This tutorial shows how to retrieve a <namespace:>'s properties, its <subnamespaces:>, and the <mosaics:> defined under
it.

## Prerequisites

This tutorial only reads data from the network. No account is required.

Before you start, make sure to [set up your development environment](../start/setup.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/get_namespace_info', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default <testnet:> node is used.

The `NAMESPACE_NAME` environment variable specifies which namespace to query, given as its full dot-separated
[name](../../textbook/namespaces.md#name) like `foo` or `foo.bar`.
If not set, it defaults to `company`, a <root namespace:> registered on testnet.

## Code Explanation

### Fetching Namespace Information

{{ tutorial.code_snippet_tagged('step-1') }}

The <get:/namespace> endpoint retrieves the current properties of a namespace, including:

* **Name:** The complete dot-separated [identifier](../../textbook/namespaces.md#name) of the namespace,
    from the root down to the queried level.
    For example, `foo` is a <root namespace:> and `foo.bar` is a <subnamespace:> of `foo`.

    `fqn` in the returned data structure stands for _Fully-Qualified Name_.

* **Owner:** The <address:> of the account that [registered the namespace](../../textbook/namespaces.md#ownership).

* **Height:** The <block:> height at which the current ownership began.

### Computing the Lease Expiration

{{ tutorial.code_snippet_tagged('step-2') }}

Namespaces are not owned permanently.
A root namespace is [leased](../../textbook/namespaces.md#duration) for 525600 blocks (approximately one year)
and must be renewed before it expires.
Subnamespaces are not leased individually, as they expire together with their root namespace.

The expiration height is not part of the API response, but it can be derived by adding the lease duration to the
namespace's height.
Comparing it with the current chain height, returned by <get:/chain/height>, gives the number of blocks remaining before
the namespace expires.

### Listing Subnamespaces

{{ tutorial.code_snippet_tagged('step-3') }}

There is no endpoint that returns the children of a namespace directly.
However, because [subnamespaces always share the owner](../../textbook/namespaces.md#ownership) of their root
namespace, they can be found by querying the namespaces owned by that account.

The <get:/account/namespace/page> endpoint returns the namespaces owned by an account, and its optional `parent`
parameter restricts the results to subnamespaces of a given namespace.
Using the namespace owner obtained in the previous step and the queried namespace as the `parent` value returns its
subnamespaces.

### Listing the Namespace's Mosaics

{{ tutorial.code_snippet_tagged('step-4') }}

Mosaics are always [defined under a namespace](../../textbook/mosaics.md#fully-qualified-name), which acts as a prefix
grouping related mosaics together.

The <get:/namespace/mosaic/definition/page> endpoint returns one definition for each mosaic whose namespace matches
the queried name exactly.
Mosaics defined under deeper subnamespaces (such as `foo.bar:baz` when querying `foo`) are not included.

## Output

The output shown below corresponds to a typical run of the program, querying the `company` namespace on testnet.

```text linenums="1" hl_lines="5 6 7 9 10 11 14 15 18 19"
--8<-- 'devbook/namespaces/get_namespace_info.log'
```

Some highlights from the output:

* **Namespace name** (line 5): The queried namespace, `company`.
    Because it contains no dots, it is a root namespace.

* **Owner** (line 6): The account that currently owns the namespace.

* **Height** (line 7): The block height at which the current ownership period began.

* **Lease expiration** (lines 9-11): The expiration height is the ownership height plus the lease duration of
    525600 blocks.
    Subtracting the current chain height shows how many blocks remain before expiration.

* **Subnamespaces** (lines 14-15): One subnamespace exists under `company`: `company.division`.

* **Mosaics** (lines 18-19): One mosaic is defined directly under the namespace: `company:token`.

## Conclusion

This tutorial showed how to:

| Step                                                             | Related documentation                    |
| ---------------------------------------------------------------- | ---------------------------------------- |
| [Fetch namespace properties](#fetching-namespace-information)    | <get:/namespace>                         |
| [Compute the lease expiration](#computing-the-lease-expiration)  | <get:/chain/height>                      |
| [List subnamespaces](#listing-subnamespaces)                     | <get:/account/namespace/page>            |
| [List the namespace's mosaics](#listing-the-namespaces-mosaics)  | <get:/namespace/mosaic/definition/page>  |
