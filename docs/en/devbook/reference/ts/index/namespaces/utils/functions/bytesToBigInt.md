# Function: bytesToBigInt()

```ts
function bytesToBigInt(
   input, 
   size, 
   isSigned?): bigint
```

Converts aligned bytes to little-endian number.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `input` | `Uint8Array` | Uint8 array. |
| `size` | `number` | Number of bytes. |
| `isSigned`? | `boolean` | \c true if number should be treated as signed. |

## Returns

`bigint`

Value corresponding to the input.
