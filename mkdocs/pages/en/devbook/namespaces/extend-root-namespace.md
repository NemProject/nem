---
title: Extend Root Namespace
tutorial_level: beginner
---

# Extending a Root Namespace

<Root namespaces:> are leased for a limited [duration](../../textbook/namespaces.md#duration) of approximately one year
per registration.
If you want to keep a namespace beyond its initial lease, you need to extend it.

This tutorial shows how to extend a root namespace.

## Prerequisites

* An <account:> that owns an active root namespace.
    See [Registering a Root Namespace](./register-root-namespace.md).
* <XEM:> to pay for the transaction and lease fees.

## When to Extend

You can extend a namespace in two situations:

* **Near the end of the lease:** Renewal is only allowed during the final 43200 blocks (approximately 30 days) before
    expiration.
    The network rejects renewal attempts made earlier than that.

* **During the grace period:** The namespace has expired but is still within the
    [grace period](../../textbook/namespaces.md#duration).
    Extending it restores the namespace to active status immediately.

!!! note "Extending Subnamespaces"

    Only root namespaces need to be extended.
    <Subnamespaces:> inherit the duration of their root namespace, so when you extend a root namespace, all its
    subnamespaces are automatically extended as well.

## Procedure

To extend a namespace, repeat the [registration process](./register-root-namespace.md) using the
**same root namespace name** and pay the **100 XEM lease fee** again.

The account signing the transaction must be the namespace owner.

The protocol accepts renewals only [near the end of the lease or during the grace period](#when-to-extend).

## Duration and Limits

Each renewal extends the lease to one year after the block containing the renewal transaction, not one year after the
previous expiration.
As a consequence, a namespace cannot be prepaid for multiple years in advance.

To maintain a namespace indefinitely, renew it during the renewal window every year.

For more details, see [Duration](../../textbook/namespaces.md#duration) in the Textbook.
