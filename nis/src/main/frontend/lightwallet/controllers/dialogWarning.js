'use strict';

define([
    'definitions',
	'jquery',

    'filters/filters',
], function(angular, $) {
	var mod = angular.module('walletApp.controllers');

	mod.controller('DialogWarningCtrl',
	    ["$scope", 'warningMsg',
        function($scope, warningMsg) {
            $scope.warningMsg = warningMsg;
        }
    ]);
});