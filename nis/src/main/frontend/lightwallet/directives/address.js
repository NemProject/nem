'use strict';
define([
    'definitions',
    'utils/Address'
], function(angular, Address) {

    var mod = angular.module('walletApp.controllers');

    /**
     * This directive does not use isolated scope, so we will have access to scope.network
     */
    mod.directive('addressField', function() {
        return {
            require: 'ngModel',
            link: function(scope, elm, attrs, ctrl) {
                ctrl.$validators.addressField = function(modelValue, viewValue) {
                    if (ctrl.$isEmpty(modelValue)) {
                        return false;
                    }
                    return Address.isValid(modelValue);
                };
            }
        };
    });

    return mod;
});