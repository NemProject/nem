'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',

	'filters/filters',
	'services/Transactions'
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('TxMosaicSupplyCtrl',
	    ["$scope", "$localStorage", "Transactions", 'walletScope',
        function($scope, $localStorage, Transactions, walletScope) {
            $scope.$storage = $localStorage.$default({'txMosaicSupplyDefaults':{}});
            $scope.walletScope = walletScope;

            // begin tracking currently selected account and it's mosaics
            $scope._updateCurrentAccount = function() {
                var acct = $scope.walletScope.accountData.account.address
                if ($scope.txMosaicSupplyData.isMultisig) {
                    acct = $scope.txMosaicSupplyData.multisigAccount.address;
                }
                $scope.currentAccount = acct;
            };

            $scope.selectTab = function selectTab(v) {
                if (v === 'multisig') {
                    $scope.txMosaicSupplyData.isMultisig = true;
                } else {
                    $scope.txMosaicSupplyData.isMultisig = false;
                }
                $scope.updateCurrentAccountMosaics();
            };

            $scope.updateCurrentAccountMosaics = function updateCurrentAccountMosaics() {
                $scope._updateCurrentAccount();
                var acct = $scope.currentAccount;
                $scope.currentAccountMosaicNames = Object.keys($scope.walletScope.mosaicOwned[acct]).sort();
                $scope.selectedMosaic = "nem:xem";
            };
            // end begin tracking currently selected account and it's mosaics

            // load data from storage
            $scope.common = {
                'requiresKey': $scope.walletScope.sessionData.getRememberedKey() === undefined,
                'password': '',
                'privatekey': '',
            };
            $scope.txMosaicSupplyData = {
                'mosaic': '',
                'supplyType': 1,
                'delta': 0,
                'fee': 0,
                'innerFee': 0,
                'due': $scope.$storage.txMosaicSupplyDefaults.due || 60,
                'isMultisig': ($scope.$storage.txMosaicSupplyDefaults.isMultisig && walletScope.accountData.meta.cosignatoryOf.length > 0) || false,
                'multisigAccount': walletScope.accountData.meta.cosignatoryOf.length == 0?'':walletScope.accountData.meta.cosignatoryOf[0]
            };

            function updateFee() {
                var entity = Transactions.prepareMosaicSupply($scope.common, $scope.txMosaicSupplyData);
                $scope.txMosaicSupplyData.fee = entity.fee;
                if ($scope.txMosaicSupplyData.isMultisig) {
                    $scope.txMosaicSupplyData.innerFee = entity.otherTrans.fee;
                }
            }

            $scope.$watchGroup(['txMosaicSupplyData.isMultisig'], function(nv, ov){
                updateFee();
            });
            $scope.$watchGroup(['common.password', 'common.privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });
            $scope.$watch('selectedMosaic', function(){
                $scope.txMosaicSupplyData.mosaic = $scope.walletScope.mosaicOwned[$scope.currentAccount][$scope.selectedMosaic].mosaicId;
            });

            $scope.updateCurrentAccountMosaics();

            $scope.ok = function () {
                $scope.$storage.txMosaicSupplyDefaults.due = $scope.txMosaicSupplyData.due;
                $scope.$storage.txMosaicSupplyDefaults.isMultisig = $scope.txMosaicSupplyData.isMultisig;

                var rememberedKey = $scope.walletScope.sessionData.getRememberedKey();
                if (rememberedKey) {
                    $scope.common.privatekey = CryptoHelpers.decrypt(rememberedKey);
                } else {
                    if (! CryptoHelpers.passwordToPrivatekey($scope.common, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                        $scope.invalidKeyOrPassword = true;
                        return;
                    }
                }
                var entity = Transactions.prepareMosaicSupply($scope.common, $scope.txMosaicSupplyData);
                Transactions.serializeAndAnnounceTransaction(entity, $scope.common, $scope.txMosaicSupplyData, $scope.walletScope.nisPort,
                    function(data) {
                        if (data.status === 200) {
                            if (data.data.code >= 2) {
                                alert('failed when trying to send tx: ' + data.data.message);
                            } else {
                                $scope.$close();
                            }
                            if (rememberedKey) { delete $scope.common.privatekey; }
                        }
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