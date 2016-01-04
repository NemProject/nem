'use strict';

define([
    'definitions',
    'jquery',
    'utils/CryptoHelpers',
    'utils/KeyPair',
    'utils/TransactionType',
    'utils/convert',
    'services/SessionData'
], function(angular, $, CryptoHelpers, KeyPair, TransactionType, convert){
    var mod = angular.module('walletApp.services');
    mod.factory('Transactions', ['$http', '$location', 'sessionData',
            function TransactionsFactory($http, $location, sessionData) {
        var o = {
        };

        var NEM_EPOCH = Date.UTC(2015, 2, 29, 0, 6, 25, 0);
        var CURRENT_NETWORK_ID = sessionData.getNetworkId();
        var CURRENT_NETWORK_VERSION = function(val) {
            if (CURRENT_NETWORK_ID === 104) {
                return 0x68000000 | val;
            } else if (CURRENT_NETWORK_ID === -104) {
                return 0x98000000 | val;
            }
            return val;
        };

        function CREATE_DATA(txtype, senderPublicKey, timeStamp, due, version)
        {
            return {
                'type': txtype,
                'version': version || CURRENT_NETWORK_VERSION(1),
                'signer': senderPublicKey,
                'timeStamp': timeStamp,
                'deadline': timeStamp + due * 60
            };
        }

        function CALC_MIN_FEE(numNem) {
            return Math.ceil(Math.max(10 - numNem, 2, Math.floor(Math.atan(numNem / 150000.0) * 3 * 33)));
        }

        o.getTimeStamp = function() {
            return Math.floor((Date.now() / 1000) - (NEM_EPOCH / 1000));
        };

        o._constructTransfer = function(senderPublicKey, recipientCompressedKey, amount, message, due, mosaics, mosaicsFee) {
            var timeStamp = o.getTimeStamp();
            var version = mosaics ? CURRENT_NETWORK_VERSION(2) : CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x101, senderPublicKey, timeStamp, due, version);

            var msgFee = message.payload.length ? Math.max(1, Math.floor(message.payload.length / 2 / 16)) * 2 : 0;
            var fee = mosaics ? mosaicsFee : CALC_MIN_FEE(amount / 1000000);
            var totalFee = (msgFee + fee) * 1000000;
            var custom = {
                'recipient': recipientCompressedKey.toUpperCase().replace(/-/g, ''),
                'amount': amount,
                'fee': totalFee,
                'message': message,
                'mosaics': mosaics
            };
            var entity = $.extend(data, custom);
            //console.log(entity);
            return entity;
        };

        //actualSender, namespaceParent, namespaceName
        o._constructNamespace = function(senderPublicKey, rentalFeeSink, rentalFee, namespaceParent, namespaceName, due) {
            var timeStamp = o.getTimeStamp();
            var version = CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x2001, senderPublicKey, timeStamp, due, version);

            var fee = 2 * 3 * 18;
            var totalFee = (fee) * 1000000;
            var custom = {
                'rentalFeeSink': rentalFeeSink.toUpperCase().replace(/-/g, ''),
                'rentalFee': rentalFee,
                'parent': namespaceParent,
                'newPart': namespaceName,
                'fee': totalFee
            };
            var entity = $.extend(data, custom);
            //console.log(entity);
            return entity;
        };

        o._constructMosaicDefinition = function(senderPublicKey, rentalFeeSink, rentalFee, namespaceParent, mosaicName, mosaicDescription, mosaicProperties, levy, due) {
            var timeStamp = o.getTimeStamp();
            var version = CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x4001, senderPublicKey, timeStamp, due, version);

            var fee = 2 * 3 * 18;
            var totalFee = (fee) * 1000000;
            var levyData = levy ? {
                'type': levy.feeType,
                'recipient': levy.address.toUpperCase().replace(/-/g, ''),
                'mosaicId': levy.mosaic,
                'fee': levy.fee,
            } : null;
            var custom = {
                'creationFeeSink': rentalFeeSink.replace(/-/g, ''),
                'creationFee': rentalFee,
                'mosaicDefinition':{
                    'creator': senderPublicKey,
                    'id': {
                        'namespaceId': namespaceParent,
                        'name': mosaicName,
                    },
                    'description': mosaicDescription,
                    'properties': $.map(mosaicProperties, function(v,k){
                        return {'name':k, 'value':v.toString()};
                    }),
                    'levy': levyData
                },
                'fee': totalFee
            };
            var entity = $.extend(data, custom);
            //console.log(entity);
            return entity;
        };

        o._constructMosaicSupply = function(senderPublicKey, mosaicId, supplyType, delta, due) {
            var timeStamp = o.getTimeStamp();
            var version = CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x4002, senderPublicKey, timeStamp, due, version);

            var fee = 2 * 3 * 18;
            var totalFee = (fee) * 1000000;
            var custom = {
                'mosaicId': mosaicId,
                'supplyType': supplyType,
                'delta': delta,
                'fee': totalFee
            };
            var entity = $.extend(data, custom);
            //console.log(entity);
            return entity;
        };

        o._constructSignature = function(senderPublicKey, otherAccount, otherHash, due) {
            var timeStamp = o.getTimeStamp();
            var version = CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x1002, senderPublicKey, timeStamp, due, version);
            var totalFee = (2 * 3) * 1000000;
            var custom = {
                'otherHash': { 'data': otherHash },
                'otherAccount': otherAccount,
                'fee': totalFee,
            };
            var entity = $.extend(data, custom);
            return entity;
        };

        o._multisigWrapper = function(senderPublicKey, innerEntity, due) {
            var timeStamp = o.getTimeStamp();
            var version = CURRENT_NETWORK_VERSION(1);
            var data = CREATE_DATA(0x1004, senderPublicKey, timeStamp, due, version);
            var custom = {
                'fee': 18000000,
                'otherTrans': innerEntity
            };
            var entity = $.extend(data, custom);
            //console.log("_multisigWrapper: ", entity);
            return entity;
        };

        /**
         * NOTE, related to serialization: Unfortunately we need to create few objects
         * and do a bit of copying, as Uint32Array does not allow random offsets
         */

        /* safe string - each char is 8 bit */
        o._serializeSafeString = function(str) {
            var r = new ArrayBuffer(132);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);

            var e = 4;
            if (str === null) {
                d[0] = 0xffffffff;

            } else {
                d[0] = str.length;
                for (var j = 0; j < str.length; ++j) {
                    b[e++] = str.charCodeAt(j);
                }
            }
            return new Uint8Array(r, 0, e);
        };
        o._serializeUaString = function(str) {
            var r = new ArrayBuffer(516);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);

            var e = 4;
            if (str === null) {
                d[0] = 0xffffffff;

            } else {
                d[0] = str.length;
                for (var j = 0; j < str.length; ++j) {
                    b[e++] = str[j];
                }
            }
            return new Uint8Array(r, 0, e);
        };
        o._serializeLong = function(value) {
            var r = new ArrayBuffer(8);
            var d = new Uint32Array(r);
            d[0] = value;
            d[1] = Math.floor((value / 0x100000000));
            return new Uint8Array(r, 0, 8);
        };
        o._serializeMosaicId = function(mosaicId) {
            var r = new ArrayBuffer(264);
            var serializedNamespaceId = o._serializeSafeString(mosaicId.namespaceId);
            var serializedName = o._serializeSafeString(mosaicId.name);

            var b = new Uint8Array(r);
            var d = new Uint32Array(r);
            d[0] = serializedNamespaceId.length + serializedName.length;
            var e = 4;
            for (var j=0; j<serializedNamespaceId.length; ++j) {
                b[e++] = serializedNamespaceId[j];
            }
            for (var j=0; j<serializedName.length; ++j) {
                b[e++] = serializedName[j];
            }
            return new Uint8Array(r, 0, e);
        };
        o._serializeMosaicAndQuantity = function(mosaic) {
            var r = new ArrayBuffer(4 + 264 + 8);
            var serializedMosaicId = o._serializeMosaicId(mosaic.mosaicId);
            var serializedQuantity = o._serializeLong(mosaic.quantity);

            //console.log(convert.ua2hex(serializedQuantity), serializedMosaicId, serializedQuantity);

            var b = new Uint8Array(r);
            var d = new Uint32Array(r);
            d[0] = serializedMosaicId.length + serializedQuantity.length;
            var e = 4;
            for (var j=0; j<serializedMosaicId.length; ++j) {
                b[e++] = serializedMosaicId[j];
            }
            for (var j=0; j<serializedQuantity.length; ++j) {
                b[e++] = serializedQuantity[j];
            }
            return new Uint8Array(r, 0, e);
        };
        o._serializeMosaics = function(entity) {
            var r = new ArrayBuffer(276*10 + 4);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);

            var i = 0;
            var e = 0;

            d[i++] = entity.length;
            e += 4;

            var temporary = [];
            for (var j=0; j<entity.length; ++j) {
                temporary.push({'entity':entity[j], 'value':mosaicIdToName(entity[j].mosaicId) + " : " + entity[j].quantity})
            }
            temporary.sort(function(a, b) {return a.value < b.value ? -1 : a.value > b.value;});

            for (var j=0; j<temporary.length; ++j) {
                var entity = temporary[j].entity;
                var serializedMosaic = o._serializeMosaicAndQuantity(entity);
                for (var k=0; k<serializedMosaic.length; ++k) {
                    b[e++] = serializedMosaic[k];
                }
            }

            return new Uint8Array(r, 0, e);
        };
        o._serializeProperty = function(entity) {
            var r = new ArrayBuffer(1024);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);
            var serializedName = o._serializeSafeString(entity['name']);
            var serializedValue = o._serializeSafeString(entity['value']);
            d[0] = serializedName.length + serializedValue.length;
            var e = 4;
            for (var j = 0; j<serializedName.length; ++j) { b[e++] = serializedName[j]; }
            for (var j = 0; j<serializedValue.length; ++j) { b[e++] = serializedValue[j]; }
            return new Uint8Array(r, 0, e);
        };
        o._serializeProperties = function(entity) {
            var r = new ArrayBuffer(1024);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);

            var i = 0;
            var e = 0;

            d[i++] = entity.length;
            e += 4;

            var temporary = entity;

            var temporary = [];
            for (var j=0; j<entity.length; ++j) {
                temporary.push(entity[j]);
            }

            var helper = {'divisibility':1, 'initialSupply':2, 'supplyMutable':3, 'transferable':4};
            temporary.sort(function(a, b) {return helper[a.name] < helper[b.name] ? -1 : helper[a.name] > helper[b.name];});

            for (var j=0; j<temporary.length; ++j) {
                var entity = temporary[j];
                var serializedProperty = o._serializeProperty(entity);
                for (var k=0; k<serializedProperty.length; ++k) {
                    b[e++] = serializedProperty[k];
                }
            }
            return new Uint8Array(r, 0, e);
        };
        o._serializeLevy = function(entity) {
            var r = new ArrayBuffer(1024);
            var d = new Uint32Array(r);

            if (entity === null)
            {
                d[0] = 0;
                return new Uint8Array(r, 0, 4);
            }

            var b = new Uint8Array(r);
            d[1] = entity['type'];

            var e = 8;
            var temp = o._serializeSafeString(entity['recipient']);
            for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

            var serializedMosaicId = o._serializeMosaicId(entity['mosaicId']);
            for (var j=0; j<serializedMosaicId.length; ++j) {
                b[e++] = serializedMosaicId[j];
            }

            var serializedFee = o._serializeLong(entity['fee']);
            for (var j=0; j<serializedFee.length; ++j) {
                b[e++] = serializedFee[j];
            }

            d[0] = 4 + temp.length + serializedMosaicId.length + 8;

            return new Uint8Array(r, 0, e);
        };
        o._serializeMosaicDefinition = function(entity) {
            var r = new ArrayBuffer(40 + 264 + 516 + 1024 + 1024);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);

            var temp = convert.hex2ua(entity['creator']);
            d[0] = temp.length;
            var e = 4;
            for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

            var serializedMosaicId = o._serializeMosaicId(entity.id);
            for (var j=0; j<serializedMosaicId.length; ++j) {
                b[e++] = serializedMosaicId[j];
            }

            var utf8ToUa = convert.hex2ua(convert.utf8ToHex(entity['description']));
            var temp = o._serializeUaString(utf8ToUa);
            for (var j=0; j<temp.length; ++j) {
                b[e++] = temp[j];
            }

            var temp = o._serializeProperties(entity['properties']);
            for (var j=0; j<temp.length; ++j) {
                b[e++] = temp[j];
            }

            var levy = o._serializeLevy(entity['levy']);
            for (var j=0; j<levy.length; ++j) {
                b[e++] = levy[j];
            }
            return new Uint8Array(r, 0, e);
        };

        o.serializeTransaction = function(entity) {
            var r = new ArrayBuffer(512 + 2764);
            var d = new Uint32Array(r);
            var b = new Uint8Array(r);
            d[0] = entity['type'];
            d[1] = entity['version'];
            d[2] = entity['timeStamp'];

            var temp = convert.hex2ua(entity['signer']);
            d[3] = temp.length;
            var e = 16;
            for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

            // Transaction
            var i = e / 4;
            d[i++] = entity['fee'];
            d[i++] = Math.floor((entity['fee'] / 0x100000000));
            d[i++] = entity['deadline'];
            e += 12;

            // TransferTransaction
            if (d[0] === TransactionType.Transfer) {
                d[i++] = entity['recipient'].length;
                e += 4;
                // TODO: check that entity['recipient'].length is always 40 bytes
                for (var j = 0; j < entity['recipient'].length; ++j) {
                    b[e++] = entity['recipient'].charCodeAt(j);
                }
                i = e / 4;
                d[i++] = entity['amount'];
                d[i++] = Math.floor((entity['amount'] / 0x100000000));
                e += 8;

                if (entity['message']['type'] === 1 || entity['message']['type'] === 2) {
                    var temp = convert.hex2ua(entity['message']['payload']);
                    if (temp.length === 0) {
                        d[i++] = 0;
                        e += 4;
                    } else {
                        // length of a message object
                        d[i++] = 8 + temp.length;
                        // object itself
                        d[i++] = entity['message']['type'];
                        d[i++] = temp.length;
                        e += 12;
                        for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }
                    }
                }

                var entityVersion = d[1] & 0xffffff;
                if (entityVersion >= 2) {
                    var temp = o._serializeMosaics(entity['mosaics']);
                    for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }
                }

            // Provision Namespace transaction
            } else if (d[0] === TransactionType.ProvisionNamespace) {
                d[i++] = entity['rentalFeeSink'].length;
                e += 4;
                // TODO: check that entity['rentalFeeSink'].length is always 40 bytes
                for (var j = 0; j < entity['rentalFeeSink'].length; ++j) {
                    b[e++] = entity['rentalFeeSink'].charCodeAt(j);
                }
                i = e / 4;
                d[i++] = entity['rentalFee'];
                d[i++] = Math.floor((entity['rentalFee'] / 0x100000000));
                e += 8;

                var temp = o._serializeSafeString(entity['newPart']);
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

                var temp = o._serializeSafeString(entity['parent']);
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

            // Mosaic Definition Creation transaction
            } else if (d[0] === TransactionType.MosaicDefinition) {
                var temp = o._serializeMosaicDefinition(entity['mosaicDefinition']);
                d[i++] = temp.length;
                e += 4;
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

                temp = o._serializeSafeString(entity['creationFeeSink']);
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

                temp = o._serializeLong(entity['creationFee']);
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }

            // Mosaic Supply Change transaction
            } else if (d[0] === TransactionType.MosaicSupply) {
                var serializedMosaicId = o._serializeMosaicId(entity['mosaicId']);
                for (var j=0; j<serializedMosaicId.length; ++j) {
                    b[e++] = serializedMosaicId[j];
                }

                var temp = new ArrayBuffer(4);
                d = new Uint32Array(temp);
                d[0] = entity['supplyType'];
                var serializeSupplyType = new Uint8Array(temp);
                for (var j=0; j<serializeSupplyType.length; ++j) {
                    b[e++] = serializeSupplyType[j];
                }

                var serializedDelta = o._serializeLong(entity['delta']);
                for (var j=0; j<serializedDelta.length; ++j) {
                    b[e++] = serializedDelta[j];
                }

            // Signature transaction
            } else if (d[0] === TransactionType.MultisigSignature) {
                var temp = convert.hex2ua(entity['otherHash']['data']);
                // length of a hash object....
                d[i++] = 4 + temp.length;
                // object itself
                d[i++] = temp.length;
                e += 8;
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }
                i = e / 4;

                temp = entity['otherAccount'];
                d[i++] = temp.length;
                e += 4;
                for (var j = 0; j < temp.length; ++j) {
                    b[e++] = temp.charCodeAt(j);
                }

            // Multisig wrapped transaction
            } else if (d[0] === TransactionType.MultisigTransaction) {
                var temp = o.serializeTransaction(entity['otherTrans']);
                d[i++] = temp.length;
                e += 4;
                for (var j = 0; j<temp.length; ++j) { b[e++] = temp[j]; }
            }
            return new Uint8Array(r, 0, e);
        };

        o.prepareMessage = function prepareMessage(common, tx) {
            if (tx.encryptMessage) {
                if (!tx.recipientPubKey || !tx.message || !common.privatekey) {
                    return {'type':0, 'payload':''};
                }
                return {'type':2, 'payload':CryptoHelpers.encode(common.privatekey, tx.recipientPubKey, tx.message.toString())};
            }
            return {'type': 1, 'payload':convert.utf8ToHex(tx.message.toString())}
        };

        o.prepareTransfer = function(common, tx) {
            //console.log('prepareTransfer', tx);
            var kp = KeyPair.create(common.privatekey);
            var actualSender = tx.isMultisig ? tx.multisigAccount.publicKey : kp.publicKey.toString();
            var recipientCompressedKey = tx.recipient.toString();
            var amount = parseInt(tx.amount * 1000000, 10);
            var message = o.prepareMessage(common, tx);
            var due = tx.due;
            var mosaics = null;
            var mosaicsFee = null;
            var entity = o._constructTransfer(actualSender, recipientCompressedKey, amount, message, due, mosaics, mosaicsFee);
            if (tx.isMultisig) {
                entity = o._multisigWrapper(kp.publicKey.toString(), entity, due);
            }

            return entity;
        };

        function mosaicIdToName(mosaicId) {
            return mosaicId.namespaceId + ":" + mosaicId.name;
        }

        function calcXemEquivalent(multiplier, q, sup, divisibility) {
            if (sup === 0) {
                return 0;
            }

            // TODO: can this go out of JS (2^54) bounds? (possible BUG)
            return 8999999999 * q * multiplier / sup / Math.pow(10, divisibility + 6);
        }


        o.calculateMosaicsFee = function(multiplier, mosaics, attachedMosaics) {
            var totalFee = 0;
            for (var m of attachedMosaics) {
                // TODO: copied from filters, refactor
                var mosaicName = mosaicIdToName(m.mosaicId);
                if (!(mosaicName in mosaics)) { return ['unknown mosaic divisibility', data]; }
                var mosaicDefinitionMetaDataPair = mosaics[mosaicName];
                var divisibilityProperties = $.grep(mosaicDefinitionMetaDataPair.mosaicDefinition.properties, function(w){ return w.name === "divisibility"; });
                var divisibility = divisibilityProperties.length === 1 ? ~~(divisibilityProperties[0].value) : 0;

                //var supply = mosaicDefinitionMetaDataPair.meta.supply;
                var supply = mosaicDefinitionMetaDataPair.supply;
                var quantity = m.quantity;
                var numNem = calcXemEquivalent(multiplier, quantity, supply, divisibility);
                var fee = CALC_MIN_FEE(numNem);

                 //console.log("CALCULATING FEE for ", m, mosaicDefinitionMetaDataPair, "divisibility", divisibility, "nem equivalent", numNem, "calculated fee", fee);
                 totalFee += fee;
            }

            return (totalFee * 5) / 4;
        };

        o.prepareTransferV2 = function(common, mosaicsMetaData, tx) {
            //console.log('prepareTransferV2', tx);
            var kp = KeyPair.create(common.privatekey);
            var actualSender = tx.isMultisig ? tx.multisigAccount.publicKey : kp.publicKey.toString();
            var recipientCompressedKey = tx.recipient.toString();
            // multiplier
            var amount = parseInt(tx.multiplier * 1000000, 10);
            var message = o.prepareMessage(common, tx);
            var due = tx.due;
            var mosaics = tx.mosaics;
            var mosaicsFee = o.calculateMosaicsFee(amount, mosaicsMetaData, mosaics);
            var entity = o._constructTransfer(actualSender, recipientCompressedKey, amount, message, due, mosaics, mosaicsFee);
            if (tx.isMultisig) {
                entity = o._multisigWrapper(kp.publicKey.toString(), entity, due);
            }

            return entity;
        };

        o.prepareNamespace = function(common, tx) {
            var kp = KeyPair.create(common.privatekey);
            var actualSender = tx.isMultisig ? tx.multisigAccount.publicKey : kp.publicKey.toString();
            var rentalFeeSink = tx.rentalFeeSink.toString();
            var rentalFee = tx.rentalFee;
            var namespaceParent = tx.namespaceParent ? tx.namespaceParent.fqn : null;
            var namespaceName = tx.namespaceName.toString();
            var due = tx.due;
            var entity = o._constructNamespace(actualSender, rentalFeeSink, rentalFee, namespaceParent, namespaceName, due);
            if (tx.isMultisig) {
                entity = o._multisigWrapper(kp.publicKey.toString(), entity, due);
            }
            return entity;
        };

        o.prepareMosaicDefinition = function(common, tx) {
            var kp = KeyPair.create(common.privatekey);
            var actualSender = tx.isMultisig ? tx.multisigAccount.publicKey : kp.publicKey.toString();
            var rentalFeeSink = tx.mosaicFeeSink.toString();
            var rentalFee = tx.mosaicFee;
            var namespaceParent = tx.namespaceParent.fqn;
            var mosaicName = tx.mosaicName.toString();
            var mosaicDescription = tx.mosaicDescription.toString();
            var mosaicProperties = tx.properties;
            var levy = tx.levy.mosaic ? tx.levy : null;
            var due = tx.due;
            var entity = o._constructMosaicDefinition(actualSender, rentalFeeSink, rentalFee, namespaceParent, mosaicName, mosaicDescription, mosaicProperties, levy, due);
            if (tx.isMultisig) {
                entity = o._multisigWrapper(kp.publicKey.toString(), entity, due);
            }
            return entity;
        };

        o.prepareMosaicSupply = function(common, tx) {
            var kp = KeyPair.create(common.privatekey);
            var actualSender = tx.isMultisig ? tx.multisigAccount.publicKey : kp.publicKey.toString();
            var due = tx.due;
            var entity = o._constructMosaicSupply(actualSender, tx.mosaic, tx.supplyType, tx.delta, due);
            if (tx.isMultisig) {
                entity = o._multisigWrapper(kp.publicKey.toString(), entity, due);
            }
            return entity;
        };

        o.prepareSignature = function(common, tx, nisPort, cb, failedCb) {
            var kp = KeyPair.create(fixPrivateKey(common.privatekey));
            var actualSender = kp.publicKey.toString();
            var otherAccount = tx.multisigAccountAddress.toString();
            var otherHash = tx.hash.toString();
            var due = tx.due;
            var entity = o._constructSignature(actualSender, otherAccount, otherHash, due);
            var result = o.serializeTransaction(entity);
            var signature = kp.sign(result);
            var obj = {'data':convert.ua2hex(result), 'signature':signature.toString()};

            /*
            $http.post('http://'+$location.host()+':7890/transaction/prepare', entity).then(function (data){
                var serializedTx = data.data;
                var signature = kp.sign(serializedTx.data);

                var obj = {'data':serializedTx.data, 'signature':signature.toString()};
                console.log('nis', obj.data);
                console.log(' js', convert.ua2hex(result));

            }, function(data) {
                failedCb('prepare', data);
            });
            /*/
            $http.post('http://'+$location.host()+':'+nisPort+'/transaction/announce', obj).then(function (data){
                cb(data);
            }, function(data) {
                failedCb('announce', data);
            });
            //*/
        };

        function fixPrivateKey(privatekey) {
            return ("0000000000000000000000000000000000000000000000000000000000000000" + privatekey.replace(/^00/, '')).slice(-64);
        }

        o.serializeAndAnnounceTransaction = function(entity, common, tx, nisPort, cb, failedCb) {
            var kp = KeyPair.create(fixPrivateKey(common.privatekey));
            var result = o.serializeTransaction(entity);
            var signature = kp.sign(result);
            var obj = {'data':convert.ua2hex(result), 'signature':signature.toString()};

            /* leaving this here for testing purposes *
            $http.post('http://'+$location.host()+':7890/transaction/prepare', entity).then(function (data){
                var serializedTx = data.data;
                var signature = kp.sign(serializedTx.data);

                var obj = {'data':serializedTx.data, 'signature':signature.toString()};
                console.log('nis', obj.data);
                console.log(' js', convert.ua2hex(result));

            }, function(data) {
                failedCb('prepare', data);
            });
            /*/
            $http.post('http://'+$location.host()+':'+nisPort+'/transaction/announce', obj).then(function (data){
                cb(data);
            }, function(data) {
                failedCb('announce', data);
            });
            //*/
        };

        return o;
    }]);
});