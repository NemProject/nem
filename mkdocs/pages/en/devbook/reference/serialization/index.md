---
hide:
  - toc
---

# Serialization

## Basic Types

<div class="big-table3">
   <div id="amount"><b>Amount</b></div>
   <div>8&nbsp;ubytes</div>
   <div class="description"><p>A quantity of mosaics in absolute units. It can only be positive or zero. </p></div>
   <div id="height"><b>Height</b></div>
   <div>8&nbsp;ubytes</div>
   <div class="description"><p>Index of a block in the blockchain. The first block (the Nemesis block) has height 1 and each subsequent block increases height by 1. </p></div>
   <div id="timestamp"><b>Timestamp</b></div>
   <div>4&nbsp;ubytes</div>
   <div class="description"><p>Number of seconds elapsed since the creation of the Nemesis block. </p></div>
   <div id="address"><b>Address</b></div>
   <div>40&nbsp;ubytes</div>
   <div class="description"><p>An address identifies an account and is derived from its <a href="#publickey" title="A 32-byte (256 bit) integer derived from a private key. It serves as the public identifier of the key pair and can be disseminated widely. It is used to prove that an entity was signed with the paired private key.">PublicKey</a>. The 40 bytes correspond to its Base32-encoded form. </p></div>
   <div id="hash256"><b>Hash256</b></div>
   <div>32&nbsp;ubytes</div>
   <div class="description"><p>A 32-byte (256 bit) hash. The exact algorithm is unspecified as it can change depending on where it is used. </p></div>
   <div id="publickey"><b>PublicKey</b></div>
   <div>32&nbsp;ubytes</div>
   <div class="description"><p>A 32-byte (256 bit) integer derived from a private key. It serves as the public identifier of the key pair and can be disseminated widely. It is used to prove that an entity was signed with the paired private key. </p></div>
   <div id="signature"><b>Signature</b></div>
   <div>64&nbsp;ubytes</div>
   <div class="description"><p>A 64-byte (512 bit) array certifying that the signed data has not been modified. NEM uses Ed25519 signatures with the Keccak-512 hash function. </p></div>
</div>

## Enumerations

<a id="networktype"></a>

--8<-- 'devbook/reference/serialization/NetworkType.html'

<a id="transactiontype"></a>

--8<-- 'devbook/reference/serialization/TransactionType.html'

<a id="linkaction"></a>

--8<-- 'devbook/reference/serialization/LinkAction.html'

<a id="mosaictransferfeetype"></a>

--8<-- 'devbook/reference/serialization/MosaicTransferFeeType.html'

<a id="mosaicsupplychangeaction"></a>

--8<-- 'devbook/reference/serialization/MosaicSupplyChangeAction.html'

<a id="multisigaccountmodificationtype"></a>

--8<-- 'devbook/reference/serialization/MultisigAccountModificationType.html'

<a id="messagetype"></a>

--8<-- 'devbook/reference/serialization/MessageType.html'

<a id="blocktype"></a>

--8<-- 'devbook/reference/serialization/BlockType.html'

## Structures

<a id="transaction"></a>

--8<-- 'devbook/reference/serialization/Transaction.html'

<a id="nonverifiabletransaction"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableTransaction.html'

<a id="accountkeylinktransactionv1"></a>

--8<-- 'devbook/reference/serialization/AccountKeyLinkTransactionV1.html'

<a id="nonverifiableaccountkeylinktransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableAccountKeyLinkTransactionV1.html'

<a id="namespaceid"></a>

--8<-- 'devbook/reference/serialization/NamespaceId.html'

<a id="mosaicid"></a>

--8<-- 'devbook/reference/serialization/MosaicId.html'

<a id="mosaic"></a>

--8<-- 'devbook/reference/serialization/Mosaic.html'

<a id="sizeprefixedmosaic"></a>

--8<-- 'devbook/reference/serialization/SizePrefixedMosaic.html'

<a id="mosaiclevy"></a>

--8<-- 'devbook/reference/serialization/MosaicLevy.html'

<a id="mosaicproperty"></a>

--8<-- 'devbook/reference/serialization/MosaicProperty.html'

<a id="sizeprefixedmosaicproperty"></a>

--8<-- 'devbook/reference/serialization/SizePrefixedMosaicProperty.html'

<a id="mosaicdefinition"></a>

--8<-- 'devbook/reference/serialization/MosaicDefinition.html'

<a id="mosaicdefinitiontransactionv1"></a>

--8<-- 'devbook/reference/serialization/MosaicDefinitionTransactionV1.html'

<a id="nonverifiablemosaicdefinitiontransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableMosaicDefinitionTransactionV1.html'

<a id="mosaicsupplychangetransactionv1"></a>

--8<-- 'devbook/reference/serialization/MosaicSupplyChangeTransactionV1.html'

<a id="nonverifiablemosaicsupplychangetransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableMosaicSupplyChangeTransactionV1.html'

<a id="multisigaccountmodification"></a>

--8<-- 'devbook/reference/serialization/MultisigAccountModification.html'

<a id="sizeprefixedmultisigaccountmodification"></a>

--8<-- 'devbook/reference/serialization/SizePrefixedMultisigAccountModification.html'

<a id="multisigaccountmodificationtransactionv1"></a>

--8<-- 'devbook/reference/serialization/MultisigAccountModificationTransactionV1.html'

<a id="nonverifiablemultisigaccountmodificationtransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableMultisigAccountModificationTransactionV1.html'

<a id="multisigaccountmodificationtransactionv2"></a>

--8<-- 'devbook/reference/serialization/MultisigAccountModificationTransactionV2.html'

<a id="nonverifiablemultisigaccountmodificationtransactionv2"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableMultisigAccountModificationTransactionV2.html'

<a id="cosignaturev1body"></a>

--8<-- 'devbook/reference/serialization/CosignatureV1Body.html'

<a id="cosignaturev1"></a>

--8<-- 'devbook/reference/serialization/CosignatureV1.html'

<a id="nonverifiablecosignaturev1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableCosignatureV1.html'

<a id="sizeprefixedcosignaturev1"></a>

--8<-- 'devbook/reference/serialization/SizePrefixedCosignatureV1.html'

<a id="multisigtransactionv1"></a>

--8<-- 'devbook/reference/serialization/MultisigTransactionV1.html'

<a id="nonverifiablemultisigtransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableMultisigTransactionV1.html'

<a id="namespaceregistrationtransactionv1"></a>

--8<-- 'devbook/reference/serialization/NamespaceRegistrationTransactionV1.html'

<a id="nonverifiablenamespaceregistrationtransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableNamespaceRegistrationTransactionV1.html'

<a id="message"></a>

--8<-- 'devbook/reference/serialization/Message.html'

<a id="transfertransactionv1"></a>

--8<-- 'devbook/reference/serialization/TransferTransactionV1.html'

<a id="nonverifiabletransfertransactionv1"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableTransferTransactionV1.html'

<a id="transfertransactionv2"></a>

--8<-- 'devbook/reference/serialization/TransferTransactionV2.html'

<a id="nonverifiabletransfertransactionv2"></a>

--8<-- 'devbook/reference/serialization/NonVerifiableTransferTransactionV2.html'

<a id="block"></a>

--8<-- 'devbook/reference/serialization/Block.html'

<style>
.md-typeset h3 {
    background: var(--md-accent-fg-color--light);
    padding: 10px;
}

.md-typeset .big-table3 {
    /* Tables with lots of content in 3 columns */
    font-size: medium;
    word-break: normal;
    display: grid;
    grid-template-columns: max-content max-content auto;
    margin-top: 20px;
}

.md-typeset .big-table3 p,
.md-typeset .big-table6 p {
    margin-top: 0;
}

.md-typeset .big-table6 {
    /* Tables with lots of content in 6 columns*/
    font-size: medium;
    word-break: normal;
    display: grid;
    grid-template-columns: 10px 10px 10px minmax(min-content, 25%) minmax(min-content,25%) auto;
    margin-top: 20px;
}

/* divs inside big-table are actually cells */
.md-typeset .big-table3 div,
.md-typeset .big-table6 div {
    padding-left: 10px;
    vertical-align: top;
    border-top: 1px solid var(--md-accent-fg-color--light);
}

.md-typeset__table,
.md-typeset__table tbody {
    display: table;
    width: 100%;
    margin: 0;
}

.side-info {
    float: right;
}

.side-info td {
    background-color: var(--md-accent-fg-color--light);
}

.md-typeset table:not([class]) {
    background: transparent;
    border: none;
}
.md-typeset table:not([class]) td {
    border: none;
    padding-top: 0.25rem;
    padding-bottom: 0.25rem;
}
.md-typeset table:not([class]) td:has(dl) {
    padding-left: 0;
}
</style>
