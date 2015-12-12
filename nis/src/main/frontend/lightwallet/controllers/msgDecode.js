'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',

	'filters/filters',
], function(angular, $, CryptoHelpers) {
    var mod = angular.module('walletApp.controllers');

    mod.controller('MsgDecodeCtrl',
    ["$scope", "$localStorage", 'walletScope', 'tx',
    function($scope, $localStorage, walletScope, tx) {
        $scope.walletScope = walletScope;
        $scope.tx = tx;

        $scope.decode = {
            'password': '',
            'privatekey': '',
        };

        $scope.$watchGroup(['decode.password', 'decode.privatekey'], function(nv,ov){
            $scope.invalidKeyOrPassword = false;
        });

        $scope.decode = function () {
            if (! CryptoHelpers.passwordToPrivatekey($scope.decode, $scope.walletScope.networkId, $scope.walletScope.walletAccount) ) {
                $scope.invalidKeyOrPassword = true;
                return;
            }
            var privateKey = $scope.decode.privatekey;
            var publicKey = tx.signer;
            var payload = tx.message.payload;
            $scope.decoded = {'type':1, 'payload':CryptoHelpers.decode(privateKey, publicKey, payload) };
        };

        $scope.close = function () {
            $scope.$dismiss();
        };
    }]);
});