'use strict';
define([
    'definitions',
    'utils/Address',
    'utils/CryptoHelpers',
    'utils/KeyPair',
    'utils/NodeConnector',
    'utils/xbbcode',
    // angular related
    'controllers/dialogPassword',
    'services/SessionData',
    'directives/address'
], function(angular, Address, CryptoHelpers, KeyPair, NodeConnector, xbbcode) {
    var mod = angular.module('walletApp.controllers');

	mod.controller('LoginCtrl', ["$scope", "$localStorage", "$timeout", "$location", "$sce", "$uibModal", "sessionData",
	        function($scope, $localStorage, $timeout, $location, $sce, $uibModal, sessionData) {

        $scope.$on('$locationChangeStart', function( event ) {
            if ($scope.connector) {
                $scope.connector.close();
            }
        });
        $scope.connector = undefined;
        $scope.$storage = $localStorage.$default({});
        $scope.connectionStatus = "connecting";
        $scope.connectionData = '';
        $scope.showAll = false;
        $scope.selectedWallet = '';
        $scope.rememberMe = false;
        $scope.saltConfirmation = '·';

        // fix for old testnet accounts
        $.each($scope.$storage.wallets || [], function fixOldWallets(idx, e) {
            if (e.accounts[0].network === undefined) {
                e.accounts[0].network = -104;
            }
        });

        $scope.xbbcoded = function xbbcoded(data) {
            var htmlizedData = xbbcode.process({
                text: data,
                removeMisalignedTags: true,
                addInLineBreaks: false
            }).html
            return $sce.trustAsHtml( htmlizedData );
        };

        var connector = NodeConnector();
        connector.connect(function(){
            $scope.$apply(function(){
                $scope.connectionStatus = "checking";
                $scope.connectionData = '';
            });

            connector.on('errors', function(name, d) {
                if (d.message === "NIS_ILLEGAL_STATE_NOT_BOOTED") {
                    $timeout(function retry(){
                        connector.requestNodeInfo();
                    }, 700);
                    $scope.connectionData = '(nis not booted yet)';
                } else {
                    $scope.connectionData = '';
                    $scope.connectionStatus = "error";
                    console.log(d);
                    alert(d.error + " " + d.message);
                }
            });
            connector.on('nodeInfo', function(d) {
                $scope.$apply(function(){
                    $scope.connectionStatus = "connected";
                    $scope.connectionData = d.identity.name;
                    $scope.showAll = true;
                    $scope.network = d.metaData.networkId;
                    $scope.nisPort = d.endpoint.port;

                    sessionData.setNetworkId($scope.network);
                    sessionData.setNisPort($scope.nisPort);
                });
            });
            connector.requestNodeInfo();
        });

        $scope.displayPasswordDialog = function displayPasswordDialog(wallet, successCb) {
            var modalInstance = $uibModal.open({
                animation: false,
                templateUrl: 'views/dialogPassword.html',
                controller: 'DialogPasswordCtrl',
                backdrop: false,
                size: 'lg',
                resolve: {
                    okButtonLabel: function() {
                        return "Login";
                    },
                    wallet: function() {
                        return wallet;
                    }
                }
            });

            modalInstance.result.then(function displayPasswordDialogSuccess(priv) {
                sessionData.setRememberedKey(CryptoHelpers.encrypt(priv, CryptoHelpers.randomKey()));
                successCb();
            }, function displayPasswordDialogDismiss() {
                sessionData.setRememberedKey(undefined);
            });
        };

        $scope.removeWallet = function removeWalletDialog(wallet) {
            var modalInstance = $uibModal.open({
                animation: false,
                templateUrl: 'views/dialogPassword.html',
                controller: 'DialogPasswordCtrl',
                backdrop: false,
                size: 'lg',
                resolve: {
                    okButtonLabel: function() {
                        return "Remove";
                    },
                    wallet: function() {
                        return wallet;
                    }
                }
            });

            modalInstance.result.then(function displayPasswordDialogSuccess(priv) {
                var temp = $scope.$storage.wallets;
                $scope.$storage.wallets = $.grep(temp, function(elem){
                    //console.log(elem === wallet, elem, wallet);
                    return elem !== wallet;
                });
            }, function displayPasswordDialogDismiss() {
            });
        };

        $scope.walletLogin = function walletLogin(wallet) {
            var redirectToWallet = function redirectToWallet() { $location.path('/wallet/' + wallet.name); };
            if ($scope.rememberMe) {
                $scope.displayPasswordDialog(wallet, redirectToWallet);
            } else {
                sessionData.setRememberedKey(undefined);
                redirectToWallet();
            }
        };

        $scope.filterNetwork = function filterNetwork(elem) {
            return elem.accounts[0].network == $scope.network;
        };

        $scope.addPrivatekeyWalletHidden = true;
        $scope.addSaltedWalletHidden = true;
        $scope.addPassWalletHidden = true;
        $scope.addEncWalletHidden = true;
        $scope.showAllButtons = false;
        //$scope.$storage.wallets !== undefined;

        $scope.consoleActive = false;

        $scope.generatingInProgress = false;
        $scope.addSaltedWalletButtonText = "Create";
        $scope.addPassWalletButtonText = "Create";

        $scope.hideAll = function() {
            $scope.addPrivatekeyWalletHidden = true;
            $scope.addSaltedWalletHidden = true;
            $scope.addPassWalletHidden = true;
            $scope.addEncWalletHidden = true;
            $scope.showAllButtons = false;
        };
        $scope.showTestnetButtons = function() {
            $scope.hideAll();
            $scope.showAllButtons = true;
        };
        $scope.showAddPrivatekeyWallet = function() {
            $scope.hideAll();
            $scope.addPrivatekeyWalletHidden = false;
        };
        $scope.showAddSaltedWallet = function() {
            $scope.hideAll();
            $scope.addSaltedWalletHidden = false;
        };
        $scope.showAddPassWallet = function() {
            $scope.hideAll();
            $scope.addPassWalletHidden = false;
        };
        $scope.showAddEncWallet = function() {
            $scope.hideAll();
            $scope.addEncWalletHidden = false;
        };

        $scope.resetData = function resetData()
        {
            $scope.dummy = {'privatekey':''};
        };
        $scope.resetData();
        $scope.$watch('dummy.privatekey', function(nv,ov){
            $scope.invalidKeyOrPassword = false;
        });

        $scope.addWallet = function()
        {
            $scope.dummy.accounts[0].brain = false;
            $scope.dummy.accounts[0].network = $scope.network;
            $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
            $scope.resetData();
            $scope.hideAll();
        };

        $scope.addSaltedWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addSaltedWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.generateSaltedKey($scope.dummy.accounts[0].password);
                var k = KeyPair.create(r.priv);
                var addr = Address.toAddress(k.publicKey.toString(), $scope.network);
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pbkf2:1k";
                $scope.dummy.accounts[0].salt = r.salt;
                $scope.dummy.accounts[0].address = addr;
                $scope.dummy.accounts[0].network = $scope.network;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.resetData();
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addSaltedWalletButtonText = "Create";
                $scope.saltConfirmation = '·';
            }, 500);
        };


        $scope.addPassWallet = function()
        {
            $scope.generatingInProgress = true;
            $scope.addPassWalletButtonText = "Generating";
            $timeout(function() {
                var r = CryptoHelpers.derivePassSha($scope.dummy.accounts[0].password, 6000);
                var k = KeyPair.create(r.priv);
                var addr = Address.toAddress(k.publicKey.toString(), $scope.network);
                $scope.dummy.accounts[0].brain = true;
                $scope.dummy.accounts[0].algo = "pass:6k";
                $scope.dummy.accounts[0].salt = '';
                $scope.dummy.accounts[0].address = addr;
                $scope.dummy.accounts[0].network = $scope.network;
                delete $scope.dummy.accounts[0].password;

                $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
                $scope.resetData();
                $scope.hideAll();
                $scope.generatingInProgress = false;
                $scope.addSaltedWalletButtonText = "Create";
            }, 500);
        };

        $scope.addEncWallet = function()
        {
            $scope.dummy.accounts[0].brain = true;
            $scope.dummy.accounts[0].network = $scope.network;

            if (! CryptoHelpers.checkAddress($scope.dummy.privatekey, $scope.network, $scope.dummy.accounts[0].address))
            {
                $scope.invalidKeyOrPassword = true;
                return;
            }

            var r = CryptoHelpers.encodePrivKey($scope.dummy.privatekey, $scope.dummy.accounts[0].password);
            var addr = $scope.dummy.accounts[0].address;
            $scope.dummy.accounts[0].brain = true;
            $scope.dummy.accounts[0].algo = "pass:enc";
            $scope.dummy.accounts[0].encrypted = r.ciphertext;
            $scope.dummy.accounts[0].iv = r.iv;
            $scope.dummy.accounts[0].address = addr;
            $scope.dummy.accounts[0].network = $scope.network;
            delete $scope.dummy.accounts[0].password;
            delete $scope.dummy.privatekey;

            $localStorage.wallets = ($localStorage.wallets || []).concat($scope.dummy);
            $scope.resetData();
            $scope.hideAll();
        };
	}]);

    return mod;
});