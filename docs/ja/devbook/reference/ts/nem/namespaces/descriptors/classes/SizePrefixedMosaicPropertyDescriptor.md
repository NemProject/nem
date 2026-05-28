# Class: SizePrefixedMosaicPropertyDescriptor

Type safe descriptor used to generate a descriptor map for SizePrefixedMosaicPropertyDescriptor.

binary layout for a size prefixed mosaic property

## Constructors

### new SizePrefixedMosaicPropertyDescriptor()

```ts
new SizePrefixedMosaicPropertyDescriptor(property): SizePrefixedMosaicPropertyDescriptor
```

Creates a descriptor for SizePrefixedMosaicProperty.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `property` | [`MosaicPropertyDescriptor`](MosaicPropertyDescriptor.md) | property value |

#### Returns

`SizePrefixedMosaicPropertyDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.property` | `any` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
