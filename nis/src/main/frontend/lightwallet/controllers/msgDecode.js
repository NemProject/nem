'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',
	'utils/KeyPair',

	'filters/filters',
], function(angular, $, CryptoHelpers, KeyPair) {
    var mod = angular.module('walletApp.controllers');

    mod.controller('MsgDecodeCtrl',
    ["$scope", "$localStorage", "$http", "$location", "$timeout", 'walletScope', 'tx',
    function($scope, $localStorage, $http, $location, $timeout, walletScope, tx) {
        $scope.walletScope = walletScope;
        $scope.tx = tx;

        $scope.decode = {
            'password': '',
            'privatekey': '',
            'generatingInProgress': false,
        };

        $scope.$watchGroup(['decode.password', 'decode.privatekey'], function(nv,ov){
            $scope.invalidKeyOrPassword = false;
        });

        $scope.recipientPublicKey = '';
        $scope.gettingRecipientInfo = true;

        var nisPort = $scope.walletScope.nisPort;
        var obj = {'params':{'address':tx.recipient}};
        $http.get('http://'+$location.host()+':'+nisPort+'/account/get', obj).then(function (data){
            $scope.recipientPublicKey = data.data.account.publicKey;
            $scope.gettingRecipientInfo = false;

        }, function(data) {
            alert("couldn't obtain data from nis server");
            console.log("couldn't obtain data from nis server", tx.recipient);
            $scope.gettingRecipientInfo = false;
        });

        $scope.decode = function () {
            $scope.generatingInProgress = true;
            $timeout(function() {
                if (! CryptoHelpers.passwordToPrivatekey($scope.decode, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                    $scope.generatingInProgress = false;
                    $scope.invalidKeyOrPassword = true;
                    return;
                }

                var kp = KeyPair.create($scope.decode.privatekey);
                if (kp.publicKey.toString() === tx.signer) {
                    // sender
                    var privateKey = $scope.decode.privatekey;
                    var publicKey = $scope.recipientPublicKey;
                } else {
                    var privateKey = $scope.decode.privatekey;
                    var publicKey = tx.signer;
                }

                var payload = tx.message.payload;
                $scope.generatingInProgress = false;
                $scope.decoded = {'type':1, 'payload':CryptoHelpers.decode(privateKey, publicKey, payload) };
            }, 500);
        };

        $scope.close = function () {
            $scope.$dismiss();
        };
    }]);
});