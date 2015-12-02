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