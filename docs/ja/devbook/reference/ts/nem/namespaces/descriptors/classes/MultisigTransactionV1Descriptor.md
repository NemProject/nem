# Class: MultisigTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for MultisigTransactionV1Descriptor.

binary layout for a multisig transaction (V1, latest)

## Constructors

### new MultisigTransactionV1Descriptor()

```ts
new MultisigTransactionV1Descriptor(innerTransaction, cosignatures?): MultisigTransactionV1Descriptor
```

Creates a descriptor for MultisigTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `innerTransaction` | [`NonVerifiableTransaction`](../../models/classes/NonVerifiableTransaction.md) | inner transaction |
| `cosignatures`? | [`SizePrefixedCosignatureV1Descriptor`](SizePrefixedCosignatureV1Descriptor.md)[] | cosignatures |

#### Returns

`MultisigTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.innerTransaction` | [`NonVerifiableTransaction`](../../models/classes/NonVerifiableTransaction.md) |
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
