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
            $scope.common = {
                'requiresKey': $scope.walletScope.sessionData.getRememberedKey() === undefined,
                'password': '',
                'privatekey': '',
            };
            $scope.txCosignData = {
                'fee': hasData ? ($scope.$storage.txCosignDefaults.fee || 0): 0,
                'due': hasData ? ($scope.$storage.txCosignDefaults.due || 60): 60,
                'multisigAccount': parent.otherTrans.signer, // inner tx signer is a multisig account
                'multisigAccountAddress': Address.toAddress(parent.otherTrans.signer, $scope.walletScope.networkId),
                'hash': meta.innerHash.data, // hash of an inner tx is needed
            };

            $scope.$watchGroup(['common.password', 'common.privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });

            $scope.ok = function () {
                // save most recent data
                $scope.$storage.txCosignDefaults.fee = $scope.txCosignData.fee;
                $scope.$storage.txCosignDefaults.due = $scope.txCosignData.due;

                var rememberedKey = $scope.walletScope.sessionData.getRememberedKey();
                if (rememberedKey) {
                    $scope.common.privatekey = CryptoHelpers.decrypt(rememberedKey);
                } else {
                    if (! CryptoHelpers.passwordToPrivatekey($scope.common, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                        $scope.invalidKeyOrPassword = true;
                        return;
                    }
                }
                Transactions.prepareSignature($scope.common, $scope.txCosignData, $scope.walletScope.nisPort,
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