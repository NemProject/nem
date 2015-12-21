'use strict';

define([
    'definitions',
	'jquery',
    'utils/CryptoHelpers',

    'filters/filters',
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('DialogPasswordCtrl',
	    ["$scope", "wallet",
        function($scope, wallet) {
            $scope.invalidKeyOrPassword = false;
            $scope.password = '';
            $scope.privatekey = '';
            $scope.account = wallet.accounts[0];


            $scope.$watchGroup(['password', 'privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });

            $scope.ok = function () {
                if (! CryptoHelpers.passwordToPrivatekey($scope, $scope.account.network, $scope.account) ) {
                    $scope.invalidKeyOrPassword = true;
                    return;
                }

            };
        }
    ]);
});