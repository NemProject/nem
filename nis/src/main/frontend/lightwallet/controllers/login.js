'use strict';
define([
    'definitions',
    'utils/Address',
    'utils/CryptoHelpers',
    'utils/KeyPair',
    'utils/NodeConnector',
], function(angular, publicToAddress, CryptoHelpers, KeyPair, NodeConnector) {
    var mod = angular.module('walletApp.controllers');

	mod.controller('LoginCtrl', ["$scope", "$localStorage", "$timeout",
	        function($scope, $localStorage, $timeout) {

        $scope.$on('$locationChangeStart', function( event ) {
            if ($scope.connector) {
                $scope.connector.close();
            }
        });
        $scope.connector = undefined;
        $scope.$storage = $localStorage.$default({});

        var connector = NodeConnector();
        connector.connect(function(){
            $scope.$apply(function(){
                $scope.connectionStatus = "connected";
            });

            connector.on('errors', function(d) {
                console.log(d);
                alert(d);
            });
            connector.on('nodeInfo', function(d) {
                console.log('node info', d);
            });
            connector.requestNodeInfo();
        });

        $scope.addWalletHidden = true;
        $scope.addSaltedWalletHidden = true;
        $scope.addPassWalletHidden = $scope.$storage.wallets !== undefined;

        $scope.generatingInProgress = false;
        $scope.addSaltedWalletButtonText = "Create";
        $scope.addPassWalletButtonText = "Create";

        $scope.hideAll = function() {
            $scope.addWalletHidden = true;
            $scope.addSaltedWalletHidden = true;
            $scope.addPassWalletHidden = true;
        };
        $scope.showAddWallet = function() {
            $scope.hideAll();
            $scope.addWalletHidden = false;
        };
        $scope.showaddSaltedWallet = function() {
            $scope.hideAll();
            $scope.addSaltedWalletHidden = false;
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

        $scope.addSaltedWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addSaltedWalletButtonText = "Generating";
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
                $scope.addSaltedWalletButtonText = "Create";
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
                $scope.addSaltedWalletButtonText = "Create";
            }, 500);
        };
	}]);

    return mod;
});