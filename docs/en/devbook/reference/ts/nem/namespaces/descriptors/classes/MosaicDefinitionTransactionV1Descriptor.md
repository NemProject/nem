# Class: MosaicDefinitionTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for MosaicDefinitionTransactionV1Descriptor.

binary layout for a mosaic definition transaction (V1, latest)

## Constructors

### new MosaicDefinitionTransactionV1Descriptor()

```ts
new MosaicDefinitionTransactionV1Descriptor(
   mosaicDefinition, 
   rentalFeeSink, 
   rentalFee): MosaicDefinitionTransactionV1Descriptor
```

Creates a descriptor for MosaicDefinitionTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mosaicDefinition` | [`MosaicDefinitionDescriptor`](MosaicDefinitionDescriptor.md) | mosaic definition |
| `rentalFeeSink` | [`Address`](../../../classes/Address.md) | mosaic rental fee sink public key |
| `rentalFee` | [`Amount`](../../models/classes/Amount.md) | mosaic rental fee |

#### Returns

`MosaicDefinitionTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.mosaicDefinition` | `any` |
| `rawDescriptor.rentalFee` | [`Amount`](../../models/classes/Amount.md) |
| `rawDescriptor.rentalFeeSink` | [`Address`](../../../classes/Address.md) |
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
