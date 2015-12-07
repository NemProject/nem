'use strict';

define([
    'definitions',
	'jquery',
	'utils/Address',
	'utils/CryptoHelpers',

    'filters/filters',
	'services/Transactions'
], function(angular, $, Address, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('TxCosignatureCtrl',
	    ["$scope", "$localStorage", "Transactions", 'walletScope', 'parent', 'meta',
        function($scope, $localStorage, Transactions, walletScope, parent, meta) {
            $scope.$storage = $localStorage.$default({'txCosignDefaults':{}});
            $scope.walletScope = walletScope;

            // load data from storage
            var hasData = $scope.$storage.txCosignDefaults;
            $scope.txCosignData = {
                'fee': hasData ? ($scope.$storage.txCosignDefaults.fee || 0): 0,
                'due': hasData ? ($scope.$storage.txCosignDefaults.due || 60): 60,
                'password': '',
                'privatekey': '',
                'multisigAccount': parent.otherTrans.signer, // inner tx signer is a multisig account
                'multisigAccountAddress': Address.toAddress(parent.otherTrans.signer, $scope.walletScope.networkId),
                'hash': meta.innerHash.data, // hash of an inner tx is needed
            };

            $scope.$watchGroup(['txCosignData.password', 'txCosignData.privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });

            $scope.ok = function () {
                // save most recent data
                $scope.$storage.txCosignDefaults.fee = $scope.txCosignData.fee;
                $scope.$storage.txCosignDefaults.due = $scope.txCosignData.due;

                if (! CryptoHelpers.passwordToPrivatekey($scope.txCosignData, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                    $scope.invalidKeyOrPassword = true;
                    return;
                }
                Transactions.prepareSignature($scope.txCosignData, $scope.walletScope.nisPort,
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