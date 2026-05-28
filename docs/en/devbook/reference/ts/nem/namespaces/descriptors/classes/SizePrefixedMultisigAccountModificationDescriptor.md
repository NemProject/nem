# Class: SizePrefixedMultisigAccountModificationDescriptor

Type safe descriptor used to generate a descriptor map for SizePrefixedMultisigAccountModificationDescriptor.

binary layout for a multisig account modification prefixed with size

## Constructors

### new SizePrefixedMultisigAccountModificationDescriptor()

```ts
new SizePrefixedMultisigAccountModificationDescriptor(modification): SizePrefixedMultisigAccountModificationDescriptor
```

Creates a descriptor for SizePrefixedMultisigAccountModification.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `modification` | [`MultisigAccountModificationDescriptor`](MultisigAccountModificationDescriptor.md) | modification |

#### Returns

`SizePrefixedMultisigAccountModificationDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.modification` | `any` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
