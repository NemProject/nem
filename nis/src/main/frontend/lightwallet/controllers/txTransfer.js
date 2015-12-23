'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',

	'filters/filters',
	'services/Transactions'
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('TxTransferCtrl',
	    ["$scope", "$localStorage", "$http", "$location", "Transactions", 'walletScope',
        function($scope, $localStorage, $http, $location, Transactions, walletScope) {
            $scope.$storage = $localStorage.$default({'txTransferDefaults':{}});
            $scope.walletScope = walletScope;
            $scope.encryptDisabled = false;

            // load data from storage
            $scope.common = {
                'requiresKey': $scope.walletScope.sessionData.getRememberedKey() === undefined,
                'password': '',
                'privatekey': '',
            };
            $scope.txTransferData = {
                'recipient': $scope.$storage.txTransferDefaults.recipient || '',
                'amount': $scope.$storage.txTransferDefaults.amount,
                'fee': $scope.$storage.txTransferDefaults.fee || 0,
                'innerFee': 0,
                'due': $scope.$storage.txTransferDefaults.due || 60,
                'message': $scope.$storage.txTransferDefaults.message || '',
                'encryptMessage': $scope.$storage.txTransferDefaults.encryptMessage || false,
                'isMultisig': ($scope.$storage.txTransferDefaults.isMultisig && walletScope.accountData.meta.cosignatoryOf.length > 0) || false,
                'multisigAccount': walletScope.accountData.meta.cosignatoryOf.length == 0?'':walletScope.accountData.meta.cosignatoryOf[0]
            };

            function updateFee() {
                var entity = Transactions.prepareTransfer($scope.common, $scope.txTransferData);
                $scope.txTransferData.fee = entity.fee;
                if ($scope.txTransferData.isMultisig) {
                    $scope.txTransferData.innerFee = entity.otherTrans.fee;
                }
            }

            $scope.$watchGroup(['txTransferData.amount', 'txTransferData.message', 'txTransferData.isMultisig'], function(nv, ov){
                updateFee();
                if ($scope.txTransferData.isMultisig) {
                    $scope.txTransferData.encryptMessage = false;
                    $scope.encryptDisabled = true;
                } else {
                    $scope.encryptDisabled = false;
                }
            });

            $scope.$watchGroup(['common.password', 'common.privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });

            $scope.recipientCache = {};
            $scope.$watch('txTransferData.recipient', function(nv, ov){
                if (! nv) {
                    return;
                }

                var recipientAddress = nv.toUpperCase().replace(/-/g, '');
                var nisPort = $scope.walletScope.nisPort;
                var obj = {'params':{'address':recipientAddress}};
                if (! (recipientAddress in $scope.recipientCache)) {
                    $http.get('http://'+$location.host()+':'+nisPort+'/account/get', obj).then(function (data){
                        $scope.recipientCache[recipientAddress] = data.data.account.publicKey;
                    });
                }
            });

            $scope.ok = function () {
                // save most recent data
                // BUG: tx data is saved globally not per wallet...
                $scope.$storage.txTransferDefaults.recipient = $scope.txTransferData.recipient;
                $scope.$storage.txTransferDefaults.amount = $scope.txTransferData.amount;
                $scope.$storage.txTransferDefaults.fee = $scope.txTransferData.fee;
                $scope.$storage.txTransferDefaults.due = $scope.txTransferData.due;
                $scope.$storage.txTransferDefaults.message = $scope.txTransferData.message;
                $scope.$storage.txTransferDefaults.encryptMessage = $scope.txTransferData.encryptMessage;
                $scope.$storage.txTransferDefaults.isMultisig = $scope.txTransferData.isMultisig;

                var recipientAddress = $scope.txTransferData.recipient.toUpperCase().replace(/-/g, '');
                $scope.txTransferData.recipientPubKey = $scope.recipientCache[recipientAddress];
                if ($scope.txTransferData.encryptMessage && !$scope.txTransferData.recipientPubKey) {
                    $scope.walletScope.displayWarning("Encrypted message selected, but couldn't find public key of a recipient");
                    return;
                }


                var rememberedKey = $scope.walletScope.sessionData.getRememberedKey();
                if (rememberedKey) {
                    $scope.common.privatekey = CryptoHelpers.decrypt(rememberedKey);
                } else {
                    if (! CryptoHelpers.passwordToPrivatekey($scope.common, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                        $scope.invalidKeyOrPassword = true;
                        return;
                    }
                }

                var entity = Transactions.prepareTransfer($scope.common, $scope.txTransferData);
                Transactions.serializeAndAnnounceTransaction(entity, $scope.common, $scope.txTransferData, $scope.walletScope.nisPort,
                    function(data) {
                        if (data.status === 200) {
                            if (data.data.code >= 2) {
                                alert('failed when trying to send tx: ' + data.data.message);
                            } else {
                                $scope.$close();
                            }
                        }
                        if (rememberedKey) { delete $scope.common.privatekey; }
                    },
                    function(operation, data) {
                        // will do for now, will change it to modal later
                        alert('failed at '+operation + " " + data.data.error + " " + data.data.message);
                        if (rememberedKey) { delete $scope.common.privatekey; }
                    }
                );
            };

            $scope.cancel = function () {
                $scope.$dismiss();
            };
        }
    ]);
});