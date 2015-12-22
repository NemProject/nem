'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',

	'filters/filters',
	'services/Transactions'
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('TxTransferV2Ctrl',
	    ["$scope", "$localStorage", "Transactions", 'walletScope',
        function($scope, $localStorage, Transactions, walletScope) {
            $scope.$storage = $localStorage.$default({'txTransfer2Defaults':{}});
            $scope.walletScope = walletScope;
            $scope.counter = 1;

            // begin tracking currently selected account and it's mosaics
            $scope._updateCurrentAccount = function() {
                var acct = $scope.walletScope.accountData.account.address
                if ($scope.txTransferV2Data.isMultisig) {
                    acct = $scope.txTransferV2Data.multisigAccount.address;
                }
                $scope.currentAccount = acct;
            };

            $scope.selectTab = function selectTab(v) {
                if (v === 'multisig') {
                    $scope.txTransferV2Data.isMultisig = true;
                } else {
                    $scope.txTransferV2Data.isMultisig = false;
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

            $scope.removeMosaic = function removeMosaic(index) {
                $scope.txTransferV2Data.mosaics.splice(index, 1);
            };

            function mosaicIdToName(mosaicId) {
                return mosaicId.namespaceId + ":" + mosaicId.name;
            }

            $scope.attachMosaic = function attachMosaic() {
                var acct = $scope.currentAccount;
                var mosaic = $scope.walletScope.mosaicOwned[acct][$scope.selectedMosaic];

                var elem = $.grep($scope.txTransferV2Data.mosaics, function(w){ return mosaicIdToName(mosaic.mosaicId) === mosaicIdToName(w.mosaicId); });
                if (elem.length === 0) {
                    $scope.counter += 1;
                    $scope.txTransferV2Data.mosaics.push({'mosaicId':mosaic['mosaicId'], 'quantity':0, 'gid':'mos_id_'+$scope.counter});
                } else {
                    $('#'+elem[0].gid).focus();
                }
            };

            // load data from storage
            $scope.common = {
                'requiresKey': $scope.walletScope.sessionData.getRememberedKey() === undefined,
                'password': '',
                'privatekey': '',
            };
            $scope.txTransferV2Data = {
                'recipient': $scope.$storage.txTransfer2Defaults.recipient || '',
                'multiplier': $scope.$storage.txTransfer2Defaults.multiplier || 1,
                'amount': 0,
                'fee': $scope.$storage.txTransfer2Defaults.fee || 0,
                'innerFee': 0,
                'due': $scope.$storage.txTransfer2Defaults.due || 60,
                'message': $scope.$storage.txTransfer2Defaults.message || '',
                'isMultisig': ($scope.$storage.txTransfer2Defaults.isMultisig && walletScope.accountData.meta.cosignatoryOf.length > 0) || false,
                'multisigAccount': walletScope.accountData.meta.cosignatoryOf.length == 0?'':walletScope.accountData.meta.cosignatoryOf[0],
                'mosaics': [ {'mosaicId':{'namespaceId':'nem', 'name':'xem'}, 'quantity':0, 'gid':'mos_id_0'} ]
            };

            function updateFee() {
                var entity = Transactions.prepareTransferV2($scope.common, $scope.walletScope.mosaicDefinitionMetaDataPair, $scope.txTransferV2Data);
                $scope.txTransferV2Data.fee = entity.fee;
                if ($scope.txTransferV2Data.isMultisig) {
                    $scope.txTransferV2Data.innerFee = entity.otherTrans.fee;
                }
            }

            $scope.$watchGroup(['txTransferV2Data.message', 'txTransferV2Data.isMultisig'], function(nv, ov){
                updateFee();
            });
            $scope.$watchGroup(['common.password', 'common.privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });
            $scope.$watch('txTransferV2Data.mosaics', function(){
                updateFee();
            }, true);
            $scope.$watch('multiplier', function(){
                $scope.txTransferV2Data.amount = parseInt($scope.txTransferV2Data.multiplier * 1000000, 10) || 0;
            });

            $scope.updateCurrentAccountMosaics();

            $scope.ok = function () {
                // save most recent data
                // BUG: tx data is saved globally not per wallet...
                $scope.$storage.txTransfer2Defaults.recipient = $scope.txTransferV2Data.recipient;
                $scope.$storage.txTransfer2Defaults.multiplier = $scope.txTransferV2Data.multiplier;
                $scope.$storage.txTransfer2Defaults.fee = $scope.txTransferV2Data.fee;
                $scope.$storage.txTransfer2Defaults.due = $scope.txTransferV2Data.due;
                $scope.$storage.txTransfer2Defaults.message = $scope.txTransferV2Data.message;
                $scope.$storage.txTransfer2Defaults.isMultisig = $scope.txTransferV2Data.isMultisig;
                //

                var rememberedKey = $scope.walletScope.sessionData.getRememberedKey();
                if (rememberedKey) {
                    $scope.common.privatekey = CryptoHelpers.decrypt(rememberedKey);
                } else {
                    if (! CryptoHelpers.passwordToPrivatekey($scope.common, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                        $scope.invalidKeyOrPassword = true;
                        return;
                    }
                }

                var entity = Transactions.prepareTransferV2($scope.common, $scope.walletScope.mosaicDefinitionMetaDataPair, $scope.txTransferV2Data);
                Transactions.serializeAndAnnounceTransaction(entity, $scope.common, $scope.txTransferV2Data, $scope.walletScope.nisPort,
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