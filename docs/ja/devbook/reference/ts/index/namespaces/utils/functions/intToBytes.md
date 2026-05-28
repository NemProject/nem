# Function: intToBytes()

```ts
function intToBytes(
   value, 
   byteSize, 
   isSigned?): Uint8Array
```

Converts an integer to bytes.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `value` | `number` \| `bigint` | Integer value. |
| `byteSize` | `number` | Number of output bytes. |
| `isSigned`? | `boolean` | \c true if the value is signed. |

## Returns

`Uint8Array`

Byte representation of the integer.
