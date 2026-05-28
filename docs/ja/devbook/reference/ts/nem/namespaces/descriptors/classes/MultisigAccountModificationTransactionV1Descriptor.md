# Class: MultisigAccountModificationTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for MultisigAccountModificationTransactionV1Descriptor.

binary layout for a multisig account modification transaction (V1)

## Constructors

### new MultisigAccountModificationTransactionV1Descriptor()

```ts
new MultisigAccountModificationTransactionV1Descriptor(modifications?): MultisigAccountModificationTransactionV1Descriptor
```

Creates a descriptor for MultisigAccountModificationTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `modifications`? | [`SizePrefixedMultisigAccountModificationDescriptor`](SizePrefixedMultisigAccountModificationDescriptor.md)[] | multisig account modifications |

#### Returns

`MultisigAccountModificationTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
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
