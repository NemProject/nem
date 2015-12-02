'use strict';
define([
    'definitions',
    'utils/Address',
    'utils/CryptoHelpers',
    'utils/KeyPair',
], function(angular, publicToAddress, CryptoHelpers, KeyPair) {
    var mod = angular.module('walletApp.controllers');

	mod.controller('LoginCtrl', ["$scope", "$localStorage", "$timeout", function($scope, $localStorage, $timeout) {
        $scope.$storage = $localStorage.$default({});

        $scope.addWalletHidden = $scope.$storage.wallets !== undefined;
        $scope.addBrainWalletHidden = true;
        $scope.addPassWalletHidden = true;

        $scope.generatingInProgress = false;
        $scope.addBrainWalletButtonText = "Create";
        $scope.addPassWalletButtonText = "Create";

        $scope.hideAll = function() {
            $scope.addWalletHidden = true;
            $scope.addBrainWalletHidden = true;
            $scope.addPassWalletHidden = true;
        };
        $scope.showAddWallet = function() {
            $scope.hideAll();
            $scope.addWalletHidden = false;
        };
        $scope.showAddBrainWallet = function() {
            $scope.hideAll();
            $scope.addBrainWalletHidden = false;
        };
        $scope.showAddPassWallet = function() {
            $scope.hideAll();
            $scope.addPassWalletHidden = false;
        };

        $scope.addWallet = function()
        {
            $scope.dummy.accounts[0].brain = false;
            $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
            $scope.dummy = undefined;
            $scope.hideAll();
        };

        $scope.addBrainWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addBrainWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.generateSaltedKey($scope.dummy.accounts[0].password);
                var k = KeyPair.create(r.priv);
                var addr = publicToAddress(k.publicKey.toString());
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pbkf2:1k";
                $scope.dummy.accounts[0].salt = r.salt;
                $scope.dummy.accounts[0].address = addr;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.dummy = undefined;
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addBrainWalletButtonText = "Create";
            }, 500);
        };


        $scope.addPassWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addPassWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.derivePassSha($scope.dummy.accounts[0].password, 6000);
                var k = KeyPair.create(r.priv);
                var addr = publicToAddress(k.publicKey.toString());
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pass:6k";
                $scope.dummy.accounts[0].salt = '';
                $scope.dummy.accounts[0].address = addr;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.dummy = undefined;
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addBrainWalletButtonText = "Create";
            }, 500);
        };
	}]);

    return mod;
});