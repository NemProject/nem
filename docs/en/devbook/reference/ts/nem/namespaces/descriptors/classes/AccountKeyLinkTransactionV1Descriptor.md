# Class: AccountKeyLinkTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for AccountKeyLinkTransactionV1Descriptor.

binary layout for an account key link transaction (V1, latest)

## Constructors

### new AccountKeyLinkTransactionV1Descriptor()

```ts
new AccountKeyLinkTransactionV1Descriptor(linkAction, remotePublicKey): AccountKeyLinkTransactionV1Descriptor
```

Creates a descriptor for AccountKeyLinkTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `linkAction` | [`LinkAction`](../../models/classes/LinkAction.md) | link action |
| `remotePublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) | public key of remote account to which importance should be transferred |

#### Returns

`AccountKeyLinkTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.linkAction` | [`LinkAction`](../../models/classes/LinkAction.md) |
| `rawDescriptor.remotePublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) |
| `rawDescriptor.type` | `string` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
