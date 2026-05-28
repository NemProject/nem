# Class: SizePrefixedMosaicDescriptor

Type safe descriptor used to generate a descriptor map for SizePrefixedMosaicDescriptor.

binary layout for a mosaic with a size prefixed size

## Constructors

### new SizePrefixedMosaicDescriptor()

```ts
new SizePrefixedMosaicDescriptor(mosaic): SizePrefixedMosaicDescriptor
```

Creates a descriptor for SizePrefixedMosaic.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mosaic` | [`MosaicDescriptor`](MosaicDescriptor.md) | mosaic |

#### Returns

`SizePrefixedMosaicDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.mosaic` | `any` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
