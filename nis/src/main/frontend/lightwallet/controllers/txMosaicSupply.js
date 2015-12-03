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
            $scope.txMosaicSupplyData = {
                'mosaic': '',
                'supplyType': 1,
                'delta': 0,
                'fee': 0,
                'innerFee': 0,
                'due': $scope.$storage.txMosaicSupplyDefaults.due || 60,
                'password': '',
                'privatekey': '',
                'isMultisig': ($scope.$storage.txMosaicSupplyDefaults.isMultisig && walletScope.accountData.meta.cosignatoryOf.length > 0) || false,
                'multisigAccount': walletScope.accountData.meta.cosignatoryOf.length == 0?'':walletScope.accountData.meta.cosignatoryOf[0]
            };

            function updateFee() {
                var entity = Transactions.prepareMosaicSupply($scope.txMosaicSupplyData);
                $scope.txMosaicSupplyData.fee = entity.fee;
                if ($scope.txMosaicSupplyData.isMultisig) {
                    $scope.txMosaicSupplyData.innerFee = entity.otherTrans.fee;
                }
            }

            $scope.$watchGroup(['txMosaicSupplyData.isMultisig'], function(nv, ov){
                updateFee();
            });
            $scope.$watch('selectedMosaic', function(){
                $scope.txMosaicSupplyData.mosaic = $scope.walletScope.mosaicOwned[$scope.currentAccount][$scope.selectedMosaic].mosaicId;
            });

            $scope.updateCurrentAccountMosaics();

            $scope.ok = function () {
                $scope.$storage.txMosaicSupplyDefaults.due = $scope.txMosaicSupplyData.due;
                $scope.$storage.txMosaicSupplyDefaults.isMultisig = $scope.txMosaicSupplyData.isMultisig;

                CryptoHelpers.passwordToPrivatekey($scope.txMosaicSupplyData, $scope.walletScope.walletAccount);
                var entity = Transactions.prepareMosaicSupply($scope.txMosaicSupplyData);
                Transactions.serializeAndAnnounceTransaction(entity, $scope.txMosaicSupplyData, $scope.walletScope.nisPort,
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