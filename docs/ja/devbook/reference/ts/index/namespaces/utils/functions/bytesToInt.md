# Function: bytesToInt()

```ts
function bytesToInt(
   input, 
   size, 
   isSigned?): number
```

Converts aligned bytes to little-endian number.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `input` | `Uint8Array` | Uint8 array. |
| `size` | `number` | Number of bytes. |
| `isSigned`? | `boolean` | \c true if number should be treated as signed. |

## Returns

`number`

Value corresponding to the input.
