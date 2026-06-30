---
title: Typed Descriptors
tutorial_level: beginner
---

# Creating Transactions Using Typed Descriptors in JavaScript

{% import 'tutorial.jinja2' as tutorial with context %}
{{ tutorial.code_full_tagged('devbook/transactions/transfer_xem.typed', ['js'], show=false) }}

Transactions are a fundamental part of the NEM blockchain, because most interactions with the network happen
through them.

All JavaScript examples throughout the tutorials use the <js:TransactionFactory.create> method
to create transactions, due to its compact syntax.
However, this method is not type-safe: it accepts a generic object and depends on it having the correct fields.

This page shows how to use <js:NemFacade.createTransactionFromTypedDescriptor> instead.
This alternative accepts well-defined parameters, offering better type safety and improved IDE support.

The code presented here is the same as in the [Creating a Transfer Transaction](./transfer-xem.md) tutorial,
with the only difference being the transaction creation step.
For brevity, only that section is shown here.
The rest of the process, including signing and announcing the transaction, remains unchanged.

{{ tutorial.code_snippet_tagged('step-1') }}

[Download the full tutorial code.]({{ config.repo_url }}/raw/refs/heads/{{config.extra.nem.branch}}/mkdocs/snippets/devbook/transactions/transfer_xem.typed.mjs){ .source-link }

## Creation Process

Transactions are created in a type-safe manner in two steps: creating a transaction descriptor and creating the
transaction itself.

### Creating the Descriptor

{{ tutorial.code_snippet_tagged('step-2') }}

Typed descriptors are what provide type safety when building transactions in JavaScript,
because of their constructors with structured parameters.

See for example the <js:TransferTransactionV2Descriptor> used in the code.

Whenever one such descriptor is available, tutorials will link to both the relevant reference page and this guide.

### Creating the Transaction

{{ tutorial.code_snippet_tagged('step-3') }}

Once the descriptor is ready, creating the transaction is straightforward: it simply involves passing the descriptor to
the <js:NemFacade.createTransactionFromTypedDescriptor> method and provide the desired fees and deadline.

Note that, as in the [Creating a Transfer Transaction](./transfer-xem.md#calculating-the-transaction-fee) tutorial,
the transaction's fee must be calculated after construction because it depends on the transaction's contents.

!!! warning "Deadlines are provided differently in the typed and untyped versions"

    Deadlines passed to <js:TransactionFactory.create> are specified in seconds and are relative to the
    _network time_.
    In contrast, deadlines passed to <js:NemFacade.createTransactionFromTypedDescriptor> are specified in seconds
    and are relative to the _system time_, that is, the local clock of the machine running the code.

    This approach is convenient because it removes the need to fetch the current network time: for example,
    to make a transaction expire in two hours, you only need to provide a deadline of `#!js 2 * 60 * 60` seconds as in
    the code above.

    However, if the system clock is not properly synchronized with the network time, transactions may expire earlier
    than expected, or be rejected entirely if the provided deadline exceeds the network's maximum allowed offset of 24
    hours.

    **Therefore, applications using the type-safe method should periodically check the network time to ensure the
    system clock is properly synchronized.**

Once the transaction has been created, you can use it normally.
There is no difference between transactions created using the typed and untyped methods.
