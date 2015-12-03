'use strict';

define([
    'definitions',
	'jquery',
	'utils/CryptoHelpers',

    'filters/filters',
], function(angular, $, CryptoHelpers) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('TxDetailsCtrl',
	    ["$scope", 'walletScope', 'parent', 'tx', 'meta',
        function($scope, walletScope, parent, tx, meta) {
            $scope.walletScope = walletScope;

            $scope.parent = parent;
            $scope.tx = tx;
            $scope.meta = meta;

            $scope.ok = function () {
                $scope.$close();
            };
        }
    ]);
});