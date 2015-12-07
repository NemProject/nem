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
	    ["$scope", "$localStorage", "Transactions", 'walletScope',
        function($scope, $localStorage, Transactions, walletScope) {
            $scope.$storage = $localStorage.$default({'txTransferDefaults':{}});
            $scope.walletScope = walletScope;

            // load data from storage
            $scope.txTransferData = {
                'recipient': $scope.$storage.txTransferDefaults.recipient || '',
                'amount': $scope.$storage.txTransferDefaults.amount,
                'fee': $scope.$storage.txTransferDefaults.fee || 0,
                'innerFee': 0,
                'due': $scope.$storage.txTransferDefaults.due || 60,
                'message': $scope.$storage.txTransferDefaults.message || '',
                'password': '',
                'privatekey': '',
                'isMultisig': ($scope.$storage.txTransferDefaults.isMultisig && walletScope.accountData.meta.cosignatoryOf.length > 0) || false,
                'multisigAccount': walletScope.accountData.meta.cosignatoryOf.length == 0?'':walletScope.accountData.meta.cosignatoryOf[0]
            };

            function updateFee() {
                var entity = Transactions.prepareTransfer($scope.txTransferData);
                $scope.txTransferData.fee = entity.fee;
                if ($scope.txTransferData.isMultisig) {
                    $scope.txTransferData.innerFee = entity.otherTrans.fee;
                }
            }

            $scope.$watchGroup(['txTransferData.amount', 'txTransferData.message', 'txTransferData.isMultisig'], function(nv, ov){
                updateFee();
            });

            $scope.ok = function () {
                // save most recent data
                // BUG: tx data is saved globally not per wallet...
                $scope.$storage.txTransferDefaults.recipient = $scope.txTransferData.recipient;
                $scope.$storage.txTransferDefaults.amount = $scope.txTransferData.amount;
                $scope.$storage.txTransferDefaults.fee = $scope.txTransferData.fee;
                $scope.$storage.txTransferDefaults.due = $scope.txTransferData.due;
                $scope.$storage.txTransferDefaults.message = $scope.txTransferData.message;
                $scope.$storage.txTransferDefaults.isMultisig = $scope.txTransferData.isMultisig;
                //

                CryptoHelpers.passwordToPrivatekey($scope.txTransferData, $scope.walletScope.walletAccount);
                if (!CryptoHelpers.checkAddress($scope.txTransferData.privatekey, $scope.walletScope.networkId, $scope.walletScope.walletAccount.address))
                {
                    $scope.invalidKeyOrPassword = true;
                    return;
                }

                var entity = Transactions.prepareTransfer($scope.txTransferData);
                Transactions.serializeAndAnnounceTransaction(entity, $scope.txTransferData, $scope.walletScope.nisPort,
                    function(data) {
                        if (data.status === 200) {
                            if (data.data.code >= 2) {
                                alert('failed when trying to send tx: ' + data.data.message);
                            } else {
                                $scope.$close();
                            }
                        }
                    },
                    function(operation, data) {
                        // will do for now, will change it to modal later
                        alert('failed at '+operation + " " + data.data.error + " " + data.data.message);
                    }
                );
            };

            $scope.cancel = function () {
                $scope.$dismiss();
            };
        }
    ]);
});