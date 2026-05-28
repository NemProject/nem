# Class: SizePrefixedCosignatureV1Descriptor

Type safe descriptor used to generate a descriptor map for SizePrefixedCosignatureV1Descriptor.

cosignature attached to a multisig transaction with prefixed size

## Constructors

### new SizePrefixedCosignatureV1Descriptor()

```ts
new SizePrefixedCosignatureV1Descriptor(cosignature): SizePrefixedCosignatureV1Descriptor
```

Creates a descriptor for SizePrefixedCosignatureV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `cosignature` | [`CosignatureV1Descriptor`](CosignatureV1Descriptor.md) | cosignature |

#### Returns

`SizePrefixedCosignatureV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.cosignature` | `any` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
