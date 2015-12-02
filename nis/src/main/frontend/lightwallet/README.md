Lightwallet tech notes
======================

Lightwallet is a proof-of-concept wallet for NEM. Lightwallet implements signing of transactions,
directly inside the browser from JS (using adjusted tweetnacl-js library).

Lightwallet uses require.js, bootstrap and angular.

The most important part is WalletController.
Routes are very basic and are defined in walletApp.js.
Any angular-objects, should include 'definitions', as a first element.

The display of transactions is done using `<transaction>` directive.
The directive uses proper `line*.html` view, basing on transaction type.

Transaction's modal windows are done using angularUi and are currently handled 
via proper `tx*.js` controllers and a corresponding `tx*.html` views.

Multisig transaction details are common for multisig txes and placed directly inside
`txDetails.html` view, details for specific transaction types are in proper `details*.js` files.


nis websocket channels and data
-------------------------------

The APIs start with `/w/api/` prefix. The APIs that require an address payload needs it in json format (address must be uppercase, without delimiting hyphen  '-'):
```
{'account':'TDECM3D4JX4M2EIHMWP6PBWV4EBZY3TMAKOTO26J'}
```

Subscribtion to data for specified address should by made by sending stomp message
either to `/w/api/account/get` or `/w/api/account/subscribe`with an address payload.

Channels:

 * `/errors` - should always subscribe to errors channel, any problems will be sent there
 * `/account/<address>` - account information, this should be send whenever account state has changed
 * `/recenttransactions/<address>` - (multiple) most recent transactions for the account
 * `/transactions/<address>` - (single) confirmed tx, related to given address
 * `/unconfirmed/<address>` - (single) unconfirmed tx, related to given address
 * `/account/namespace/owned/<address>` - (single) namespace owned by given account
 * `/account/mosaic/owned/<address>` - (single) mosaics owned by given account
 * `/account/mosaic/owned/definition/<address>` - (single) definition of mosaic owned by given account 
 * `/unconfirmed` - every unconfirmed tx, that nis receives  
 * `/blocks/new` - height of a newly obtained chain fragment (this can be lower than current, height if there has been rollback) 
 * `/blocks` - full blocks
 
 APIs (preceeded by /w/api/ as stated above):
 * `/account/get` (address payload) - does an initial subscribe, and requests account info (response send to `/account/<address>`) 
 * `/account/subscribe` (address payload) - only does initial subscribe
 
Following apis are not required, but they are useful for obtaining initial state of given account.
 * `/account/transfers/all` (address payload) - requests recent BOTH confirmed and unconfirmed transactions for an account (response send to `/transactions/<address>` and `/unconfirmed/<address>`)
 * `/account/transfers/unconfirmed` (address payload) - requests unconfirmed transactions for an account (response send to `/unconfirmed/<address>`)
 * `/account/namespace/owned` (address payload) - requests namespaces owned by an account (response send to `/account/namespace/owned/<address>`)
 * `/account/mosaic/owned/definition` (address payload) - request information about mosaic definitions owned by an account  
 * `/account/mosaic/owned` (address payload) - request information about owned mosaics (name and amount held)
 