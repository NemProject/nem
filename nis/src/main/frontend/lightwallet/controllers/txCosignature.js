'use strict';

define([
    'definitions',
	'jquery',
	'utils/Address',
	'utils/CryptoHelpers',

    'filters/filters',
	'services/Transactions'
], function(angular, $, publicToAddress, CryptoHelpers) {
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
                // TODO: pass proper network byte
                'multisigAccountAddress': publicToAddress(parent.otherTrans.signer, -104),
                'hash': meta.innerHash.data, // hash of an inner tx is needed
            };

            $scope.ok = function () {
                // save most recent data
                $scope.$storage.txCosignDefaults.fee = $scope.txCosignData.fee;
                $scope.$storage.txCosignDefaults.due = $scope.txCosignData.due;

                CryptoHelpers.passwordToPrivatekey($scope.txCosignData, $scope.walletScope.walletAccount);
                Transactions.prepareSignature($scope.txCosignData,
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