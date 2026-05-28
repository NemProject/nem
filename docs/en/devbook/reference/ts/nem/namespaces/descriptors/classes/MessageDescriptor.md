# Class: MessageDescriptor

Type safe descriptor used to generate a descriptor map for MessageDescriptor.

binary layout for a message

## Constructors

### new MessageDescriptor()

```ts
new MessageDescriptor(messageType, message?): MessageDescriptor
```

Creates a descriptor for Message.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `messageType` | [`MessageType`](../../models/classes/MessageType.md) | message type |
| `message`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | message payload |

#### Returns

`MessageDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.messageType` | [`MessageType`](../../models/classes/MessageType.md) |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
