# Class: MosaicIdDescriptor

Type safe descriptor used to generate a descriptor map for MosaicIdDescriptor.

binary layout for a mosaic id

## Constructors

### new MosaicIdDescriptor()

```ts
new MosaicIdDescriptor(namespaceId, name?): MosaicIdDescriptor
```

Creates a descriptor for MosaicId.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `namespaceId` | [`NamespaceIdDescriptor`](NamespaceIdDescriptor.md) | namespace id |
| `name`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | name |

#### Returns

`MosaicIdDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.namespaceId` | `any` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
