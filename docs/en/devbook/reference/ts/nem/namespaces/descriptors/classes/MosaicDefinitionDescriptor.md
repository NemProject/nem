# Class: MosaicDefinitionDescriptor

Type safe descriptor used to generate a descriptor map for MosaicDefinitionDescriptor.

binary layout for a mosaic definition

## Constructors

### new MosaicDefinitionDescriptor()

```ts
new MosaicDefinitionDescriptor(
   ownerPublicKey, 
   id, 
   description?, 
   properties?, 
   levy?): MosaicDefinitionDescriptor
```

Creates a descriptor for MosaicDefinition.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `ownerPublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) | owner public key |
| `id` | [`MosaicIdDescriptor`](MosaicIdDescriptor.md) | mosaic id referenced by this definition |
| `description`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | description |
| `properties`? | [`SizePrefixedMosaicPropertyDescriptor`](SizePrefixedMosaicPropertyDescriptor.md)[] | properties |
| `levy`? | [`MosaicLevyDescriptor`](MosaicLevyDescriptor.md) | optional levy that is applied to transfers of this mosaic |

#### Returns

`MosaicDefinitionDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.id` | `any` |
| `rawDescriptor.ownerPublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
