'use strict';

define([
    'definitions',
	'jquery',
	'utils/Connector',
	'utils/CryptoHelpers',
    'utils/KeyPair',
	'utils/TransactionType',
    // angular related
    'controllers/dialogWarning',
    'controllers/txTransfer',
    'controllers/txTransferV2',
    'controllers/txNamespace',
    'controllers/txMosaic',
    'controllers/txMosaicSupply',
    'controllers/txCosignature',
    'controllers/txDetails',
    'controllers/msgDecode',
	'filters/filters',
    'services/Transactions',
    'services/SessionData'
], function(angular, $, Connector, CryptoHelpers, KeyPair, TransactionType) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('WalletCtrl',
	    ["$scope", "$http", "$location", "$localStorage", "$timeout", "$routeParams", "$uibModal", "sessionData",
        function($scope, $http, $location, $localStorage, $timeout, $routeParams, $uibModal, sessionData) {
            if (sessionData.getNisPort() === 0 || !sessionData.getNetworkId()) {
                $location.path('/login');
            }

            $scope.$on('$locationChangeStart', function( event ) {
                if ($scope.connector) {
                    sessionData.setRememberedKey(undefined);
                    $scope.connector.close();
                }
            });
            $scope.connector = undefined;
            $scope.$storage = $localStorage.$default({});
            $scope.$storage.txTransferDefaults = $scope.$storage.txTransferDefaults || {};
            var elem = $.grep($scope.$storage.wallets, function(w){ return w.name == $routeParams.walletName; });
            $scope.walletAccount = elem.length == 1 ? elem[0].accounts[0] : null;
            $scope.nisPort = sessionData.getNisPort();
            $scope.networkId = sessionData.getNetworkId();
            $scope.nisHeight = 0;
            $scope.sessionData = sessionData;

            $scope.activeWalletTab = 0;
            $scope.setWalletTab = function setWalletTab(index) {
                $scope.activeWalletTab = index;
            };

            function mosaicIdToName(mosaicId) {
                return mosaicId.namespaceId + ":" + mosaicId.name;
            }

            // ==== ==== ==== ==== dialogs

            // in case of dialogs, I'm passing the scope, we could ofc use $scope.$parent,
            // in descendant controllers, but this makes it more verbose and easier to follow
            // it's also easier, than passing proper elements from current scope.

            $scope.displayWarning = function(warningMsg) {
                var modalInstance = $uibModal.open({
                    animation: true,
                    templateUrl: 'views/dialogWarning.html',
                    controller: 'DialogWarningCtrl',
                    backdrop: true,
                    resolve: {
                        warningMsg: function() { return warningMsg; }
                    }
                });
            };
            $scope.displayTransferDialog = function() {
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txTransfer.html',
                    controller: 'TxTransferCtrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        }
                    }
                });
            };
            $scope.displayTransferV2Dialog = function() {
                if ($scope.networkId === 104 && $scope.nisHeight < 440000) {
                    $scope.displayWarning("v2 transfers will be available after fork at 440k");
                    return;
                }
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txTransferV2.html',
                    controller: 'TxTransferV2Ctrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        }
                    }
                });
            };
            $scope.displayNamespaceDialog = function() {
                if ($scope.networkId === 104 && $scope.nisHeight < 440000) {
                    $scope.displayWarning("namespaces will be available after fork at 440k");
                    return;
                }
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txNamespace.html',
                    controller: 'TxNamespaceCtrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        }
                    }
                });
            };
            $scope.displayMosaicDialog = function() {
                if ($scope.networkId === 104 && $scope.nisHeight < 440000) {
                    $scope.displayWarning("mosaics will be available after fork at 440k");
                    return;
                }
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txMosaic.html',
                    controller: 'TxMosaicCtrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        }
                    }
                });
            };
            $scope.displayMosaicSupplyDialog = function() {
                if ($scope.networkId === 104 && $scope.nisHeight < 440000) {
                    $scope.displayWarning("v2 transfers will be available after fork at 440k");
                    return;
                }
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txMosaicSupply.html',
                    controller: 'TxMosaicSupplyCtrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        }
                    }
                });
            };

            $scope.displayDecodeMessage = function(tx) {
                if (!tx || !tx.message || tx.message.type !== 2) {
                    alert("missing transaction data");
                    return;
                }
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/msgDecode.html',
                    controller: 'MsgDecodeCtrl',
                    backdrop: false,
                    size: 'lg',
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        },
                        tx: function() {
                            return tx;
                        }
                    }
                });
            };

            $scope.cosignTransaction = function(parentTx, tx, meta) {
                //console.log("cosignTransaction parent", parentTx, "\ncosignTransaction inner", tx, "\ncosignTransaction meta", meta);
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txCosignature.html',
                    controller: 'TxCosignatureCtrl',
                    backdrop: false,
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        },
                        parent: function() {
                            return parentTx;
                        },
                        meta: function() {
                            return meta;
                        }
                    }
                });
            };

            $scope.displayTransactionDetails = function(parentTx, tx, meta) {
                var modalInstance = $uibModal.open({
                    animation: false,
                    templateUrl: 'views/txDetails.html',
                    controller: 'TxDetailsCtrl',
                    backdrop: false,
                    size: 'lg',
                    resolve: {
                        walletScope: function() {
                            return $scope;
                        },
                        parent: function() {
                            return parentTx;
                        },
                        tx: function() {
                            return tx;
                        },
                        meta: function() {
                            return meta;
                        }
                    }
                });
            };

            // ==== ==== ==== ==== cached data

            $scope.accountData = {};
            $scope.transactions = [];
            $scope.unconfirmedSize = 0;
            $scope.unconfirmed = {};

            $scope.mosaicDefinitionMetaDataPair = {};
            $scope.mosaicDefinitionMetaDataPairSize = 0;

            $scope.mosaicOwned = {};
            $scope.mosaicOwnedSize = {};
            $scope.namespaceOwned = {};

            $scope.getLevy = function getLevy(d) {
                var mosaicName = mosaicIdToName(d.mosaicId);
                if (!(mosaicName in $scope.mosaicDefinitionMetaDataPair)) {
                    return false;
                }
                var mosaicDefinitionMetaDataPair = $scope.mosaicDefinitionMetaDataPair[mosaicName];
                return mosaicDefinitionMetaDataPair.mosaicDefinition.levy;
            };
            $scope.mosaicIdToName = mosaicIdToName;

            // ==== ==== ==== ==== connection to the server and websocket handlers

            // if we got wallet name let's set up everything...
            if (elem.length == 1) {
                $scope.name = elem[0].name;
                $scope.account = elem[0].accounts[0].address;
                $scope.connectionStatus = "connecting";

                var connector = Connector(elem[0].accounts[0].address);
                connector.connect(function(){
                    $scope.$apply(function(){
                        $scope.connectionStatus = "connected";
                    });

                    function unconfirmedCallback(d) {
                        // we could first check if coming tx is already in unconfirmed, but
                        // tx itself can change in case of multisig txes,, so don't do that check

                        $scope.$apply(function(){
                            $scope.unconfirmed[d.meta.hash.data] = d;
                            $scope.unconfirmedSize = Object.keys($scope.unconfirmed).length;
                        });
                        //console.log("unconfirmed data: ", Object.keys($scope.unconfirmed).length, d);
                        var audio = new Audio('/lightwallet/ding.ogg');
                        audio.play();
                    }

                    function confirmedCallback(d) {
                        $scope.$apply(function(){
                            delete $scope.unconfirmed[d.meta.hash.data];
                            $scope.unconfirmedSize = Object.keys($scope.unconfirmed).length;

                            $scope.transactions.push(d);
                        });
                        // console.log(">> transactions data: ", d);
                        var audio = new Audio('/lightwallet/ding2.ogg');
                        audio.play();
                    }

                    function mosaicDefinitionCallback(d) {
                        $scope.$apply(function(){
                            $scope.mosaicDefinitionMetaDataPair[mosaicIdToName(d.mosaicDefinition.id)] = d;
                            $scope.mosaicDefinitionMetaDataPairSize = Object.keys($scope.mosaicDefinitionMetaDataPair).length;
                        });
                    }

                    function mosaicCallback(d, address) {
                        $scope.$apply(function(){
                            var mosaicName = mosaicIdToName(d.mosaicId);
                             if (! (address in $scope.mosaicOwned)) {
                                $scope.mosaicOwned[address] = {};
                             }
                            $scope.mosaicOwned[address][mosaicName] = d;
                            $scope.mosaicOwnedSize[address] = Object.keys($scope.mosaicOwned[address]).length;
                        });
                    }

                    function namespaceCallback(d, address) {
                        $scope.$apply(function(){
                            var namespaceName = d.fqn;
                             if (! (address in $scope.namespaceOwned)) {
                                $scope.namespaceOwned[address] = {};
                             }
                            $scope.namespaceOwned[address][namespaceName] = d;
                        });
                    }

                    connector.on('errors', function(name, d) {
                        console.log(d);
                        alert(d.error + " " + d.message);
                    });
                    connector.on('account', function(d) {
                        $scope.$apply(function(){
                            $scope.accountData = d;
                        });
                        //console.log("account data: ", $scope.accountData);

                        // prepare callback for multisig accounts
                        for (var elem of $scope.accountData.meta.cosignatoryOf) {
                            connector.onConfirmed(confirmedCallback, elem.address);
                            connector.onUnconfirmed(unconfirmedCallback, elem.address);
                            connector.onNamespace(namespaceCallback, elem.address);
                            connector.onMosaicDefinition(mosaicDefinitionCallback, elem.address);
                            connector.onMosaic(mosaicCallback, elem.address);
                        }

                        // we need to subscribe to multisig accounts, in order to receive notifications
                        // about transactions involving those accounts
                        for (var elem of $scope.accountData.meta.cosignatoryOf) {
                            // no need to check return value, as if we're here, it means we're already connected
                            connector.subscribeToMultisig(elem.address);

                            // we don't need to request that, as request for accounts' unconfirmed txes should include those needed cosingature
                            //connector.requestUnconfirmedTransactions(elem.address);

                            connector.requestAccountNamespaces(elem.address);
                            connector.requestAccountMosaicDefinitions(elem.address);
                            connector.requestAccountMosaics(elem.address);
                        }
                    });

                    connector.on('recenttransactions', function(d) {
                        d.data.reverse();
                        $scope.$apply(function(){
                            $scope.transactions = d.data;
                        });
                        //console.log("recenttransactions data: ", d);
                    });

                    connector.on('newblocks', function(blockHeight) {
                        $scope.nisHeight = blockHeight.height;
                        var cleanedTransactions = [];
                        $.each($scope.transactions, function(idx, tx) {
                            if (tx.meta.height < blockHeight.height) {
                                cleanedTransactions.push(tx);
                            } else {
                                console.log("OK, ", blockHeight, "removed tx: ", tx);
                            }
                        });
                        $scope.$apply(function(){
                            $scope.transactions = cleanedTransactions;
                        });
                    });
                    connector.onConfirmed(confirmedCallback);
                    connector.onUnconfirmed(unconfirmedCallback);
                    connector.onNamespace(namespaceCallback);
                    connector.onMosaicDefinition(mosaicDefinitionCallback);
                    connector.onMosaic(mosaicCallback);

                    connector.requestAccountData();

                    connector.requestAccountNamespaces();
                    connector.requestAccountMosaicDefinitions();
                    connector.requestAccountTransactions();
                    connector.requestAccountMosaics();

                });
                $scope.connector = connector;
            }
        }]
    );

    function txTypeToName(id) {
        switch (id) {
            case TransactionType.Transfer: return 'Transfer';
            case TransactionType.ImportanceTransfer: return 'ImportanceTransfer';
            case TransactionType.MultisigModification: return 'MultisigModification';
            case TransactionType.ProvisionNamespace: return 'ProvisionNamespace';
            case TransactionType.MosaicDefinition: return 'MosaicDefinition';
            case TransactionType.MosaicSupply: return 'MosaicSupply';
            default: return 'Unknown_'+id;
        }
    }

    function needsSignature(multisigTransaction, accountData) {
        // we're issuer
        if (multisigTransaction.transaction.signer === accountData.account.publicKey) {
            return false;
        }
        // check if we're already on list of signatures
        for (var elem of multisigTransaction.transaction.signatures) {
            if (elem.signer === accountData.account.publicKey) {
                return false;
            }
        }
        return true;
    }

    mod.directive('tagtransaction', function() {
        return {
            restrict: 'E',
            scope: {
                d: '=',
                tooltipPosition: '=',
                accountData: '=',
            },
            // we're passing ng-include as a template in order to dynamically select proper templates,
            // the selection itself is done below in assignment to scope.templateUri
            template: '<ng-include src="templateUri"/>',
            link: function postLink(scope) {
                if (scope.d.transaction.type === 4100) {
                    scope.tx = scope.d.transaction.otherTrans;
                    scope.meta = scope.d.meta;
                    scope.parent = scope.d.transaction;
                } else {
                    scope.tx = scope.d.transaction;
                    scope.meta = scope.d.meta;
                    scope.parent = undefined;
                }

                scope.confirmed = !(scope.meta.height === Number.MAX_SAFE_INTEGER);
                // if multisig and not confirmed, check if we need to cosign
                scope.needsSignature = scope.parent && !scope.confirmed && scope.accountData && needsSignature(scope.d, scope.accountData);
                scope.templateName = txTypeToName(scope.tx.type);
                scope.templateUri = 'views/line'+scope.templateName+'.html';

                scope.cosignCallback = scope.$parent.cosignTransaction;
                scope.displayTransactionDetails = scope.$parent.displayTransactionDetails;
                scope.networkId = scope.$parent.networkId;
            }
       };
    });

    mod.directive('tagdetails', ["$http", "$location", function($http, $location) {
        return {
            restrict: 'E',
            scope: {
                parent: '=',
                tx: '=',
                meta: '='
            },
            template: '<ng-include src="templateUri"/>',
            link: function postLink(scope) {
                scope.transactionTypeName = txTypeToName(scope.tx.type);
                scope.templateUri = 'views/details'+scope.transactionTypeName+'.html';

                scope.decode = function(tx) {
                    if (tx.message && tx.message.type === 2) {
                        scope.$parent.walletScope.displayDecodeMessage(tx);
                    }
                };

                // scope.$parent == TxDetailsCtrl
                scope.mosaicDefinitionMetaDataPair = scope.$parent.walletScope.mosaicDefinitionMetaDataPair;
                scope.getLevy = scope.$parent.walletScope.getLevy;
                scope.mosaicIdToName = scope.$parent.walletScope.mosaicIdToName;
                scope.networkId = scope.$parent.walletScope.networkId;

                scope.recipientPublicKey = '';
                scope.gettingRecipientInfo = true;
                scope.requiresKey = scope.$parent.walletScope.sessionData.getRememberedKey() === undefined;

                if (!scope.requiresKey && scope.tx.type === TransactionType.Transfer && scope.tx.message && scope.tx.message.type === 2) {
                    var nisPort = scope.$parent.walletScope.nisPort;
                    var obj = {'params':{'address':scope.tx.recipient}};
                    $http.get('http://'+$location.host()+':'+nisPort+'/account/get', obj).then(function (data){
                        scope.recipientPublicKey = data.data.account.publicKey;

                        var privateKey = CryptoHelpers.decrypt(scope.$parent.walletScope.sessionData.getRememberedKey());
                        var kp = KeyPair.create(privateKey);
                        if (kp.publicKey.toString() === scope.tx.signer) {
                            // sender
                            var privateKey = privateKey;
                            var publicKey = scope.recipientPublicKey;
                        } else {
                            var privateKey = privateKey;
                            var publicKey = scope.tx.signer;
                        }

                        var payload = scope.tx.message.payload;
                        scope.decoded = {'type':1, 'payload':CryptoHelpers.decode(privateKey, publicKey, payload) };

                        scope.gettingRecipientInfo = false;

                    }, function(data) {
                        alert("couldn't obtain data from nis server");
                        console.log("couldn't obtain data from nis server", scope.tx.recipient);
                        scope.gettingRecipientInfo = false;
                    });
                }
            }
        };
    }]);

    mod.directive('taglevy', function(){
        return {
            restrict: 'E',
            scope: {
                mos: '=',
                tx: '=',
                mosaics: '='
            },
            template: '',
            transclude: true,
            compile: function compile(tElement, tAttrs, transclude) {
                return function postLink(scope, element, attrs) {
                    function mosaicIdToName(mosaicId) {
                        if (! mosaicId) return mosaicId;
                        return mosaicId.namespaceId + ":" + mosaicId.name;
                    }
                    function getLevy(d) {
                        if (!scope.mosaics) return undefined;
                        var mosaicName = mosaicIdToName(d.mosaicId);
                        if (!(mosaicName in scope.mosaics)) {
                            return undefined;
                        }
                        var mosaicDefinitionMetaDataPair = scope.mosaics[mosaicName];
                        return mosaicDefinitionMetaDataPair.mosaicDefinition.levy;
                    }
                    scope.levy = getLevy(scope.mos);

                    var foo = scope;
                    scope.$watch('mosaics', function(nv, ov) {
                        scope.levy = getLevy(scope.mos);
                        //console.log('rerender', Object.keys(scope.mosaics).length, mosaicIdToName(scope.mos.mosaicId), mosaicIdToName(scope.levy ? scope.levy.mosaicId : undefined));
                    }, true);

                    transclude(scope, function(clone, scope) {
                        element.append(clone);
                    });
                };
            }
        };
    });

    mod.directive('title', function() {
        return {
            link: function($scope, element, attrs) {
                var watch = $scope.$watch(function() {
                    return element.children().length;
                }, function() {
                    $scope.$evalAsync(function() {
                        $('[data-toggle="tooltip"]').tooltip();
                    });
                });
            },
        };
    });

    return mod;
});