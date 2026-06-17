# Function: calculateTransactionFee()

```ts
function calculateTransactionFee(transaction, mosaicInformationLookup?): bigint
```

Calculates the minimum required transaction fee for a transaction.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transaction` | [`Transaction`](../namespaces/models/classes/Transaction.md) | Transaction. |
| `mosaicInformationLookup`? | `Function` \| \{\} | Looks up mosaic information ({supply, divisibility}) given mosaic identifier. When a function, mosaic identifier will be passed as parameter. When an object map, fully qualified mosaic identifier will be used as an index. When undefined, this function will be unable to calculate fees for custom mosaic transfers. |

## Returns

`bigint`

Transaction fee.
