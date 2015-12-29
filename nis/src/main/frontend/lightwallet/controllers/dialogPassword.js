'use strict';

define([
    'definitions',
	'jquery',
    'utils/CryptoHelpers',

    'filters/filters',
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('DialogPasswordCtrl',
	    ["$scope", "$timeout", "wallet", "okButtonLabel",
        function($scope, $timeout, wallet, okButtonLabel) {
            $scope.invalidKeyOrPassword = false;
            $scope.password = '';
            $scope.privatekey = '';
            $scope.account = wallet.accounts[0];
            $scope.generatingInProgress = false;
            $scope.okButtonLabel = okButtonLabel;

            $scope.$watchGroup(['password', 'privatekey'], function(nv,ov){
                $scope.invalidKeyOrPassword = false;
            });

            $scope.ok = function () {
                $scope.generatingInProgress = true;
                var timeout = $scope.password.length > 0 ? 500 : 1;
                $timeout(function() {
                    if (! CryptoHelpers.passwordToPrivatekey($scope, $scope.account.network, $scope.account) ) {
                        $scope.generatingInProgress = false;
                        $scope.invalidKeyOrPassword = true;
                        return;
                    }

                    $scope.generatingInProgress = false;
                    $scope.$close($scope.privatekey);
                }, timeout);
            };

            $scope.cancel = function () {
                $scope.$dismiss();
            };
        }
    ]);
});