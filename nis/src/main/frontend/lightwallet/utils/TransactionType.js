'use strict';

define([],
function(){
    var TransactionType = {
        Transfer: 0x101, // 257
        ImportanceTransfer: 0x801, // 2049
        MultisigModification: 0x1001, // 4097
        MultisigSignature: 0x1002, // 4098
        MultisigTransaction: 0x1004, // 4100
        ProvisionNamespace: 0x2001, // 8193
        MosaicDefinition: 0x4001, // 16385
        MosaicSupply: 0x4002, // 16386
    };
    return TransactionType;
});