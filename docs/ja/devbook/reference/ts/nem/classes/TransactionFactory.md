# Class: TransactionFactory

Factory for creating NEM transactions.

## Constructors

### new TransactionFactory()

```ts
new TransactionFactory(network, typeRuleOverrides?): TransactionFactory
```

Creates a factory for the specified network.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `network` | [`Network`](Network.md) | NEM network. |
| `typeRuleOverrides`? | `Map`&lt;`string`, `Function`&gt; | Type rule overrides. |

#### Returns

`TransactionFactory`

## Accessors

### ruleNames

#### Get Signature

```ts
get ruleNames(): string[]
```

Gets rule names with registered hints.

##### Returns

`string`[]

Rule names with registered hints.

***

### static

#### Get Signature

```ts
get static(): typeof default
```

Gets class type.

##### Returns

*typeof* `default`

Class type.

## Methods

### create()

```ts
create(transactionDescriptor, autosort?): Transaction
```

Creates a transaction from a transaction descriptor.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transactionDescriptor` | `object` | Transaction descriptor. |
| `autosort`? | `boolean` | When set (default), descriptor arrays requiring ordering will be automatically sorted. When unset, descriptor arrays will be presumed to be already sorted. |

#### Returns

[`Transaction`](../namespaces/models/classes/Transaction.md)

Newly created transaction.

***

### attachSignature()

```ts
static attachSignature(transaction, signature): string
```

Attaches a signature to a transaction.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transaction` | [`Transaction`](../namespaces/models/classes/Transaction.md) | Transaction object. |
| `signature` | [`Signature`](../../index/classes/Signature.md) | Signature to attach. |

#### Returns

`string`

JSON transaction payload.

***

### deserialize()

```ts
static deserialize(payload): Transaction
```

Deserializes a transaction from a binary payload.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `payload` | `Uint8Array` | Binary payload. |

#### Returns

[`Transaction`](../namespaces/models/classes/Transaction.md)

Deserialized transaction.

***

### lookupTransactionName()

```ts
static lookupTransactionName(transactionType, transactionVersion): string
```

Looks up the friendly name for the specified transaction.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transactionType` | [`TransactionType`](../namespaces/models/classes/TransactionType.md) | Transaction type. |
| `transactionVersion` | `number` | Transaction version. |

#### Returns

`string`

Transaction friendly name.

***

### toJson()

```ts
static toJson(transaction): string
```

Generates a JSON representation of transaction that can be sent to a node.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transaction` | [`Transaction`](../namespaces/models/classes/Transaction.md) | Transaction object. |

#### Returns

`string`

JSON transaction payload.

***

### toNonVerifiableTransaction()

```ts
static toNonVerifiableTransaction(transaction): NonVerifiableTransaction
```

Converts a transaction to a non-verifiable transaction.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transaction` | \| [`Transaction`](../namespaces/models/classes/Transaction.md) \| [`NonVerifiableTransaction`](../namespaces/models/classes/NonVerifiableTransaction.md) | Transaction object. |

#### Returns

[`NonVerifiableTransaction`](../namespaces/models/classes/NonVerifiableTransaction.md)

Non-verifiable transaction object.
