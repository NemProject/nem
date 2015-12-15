'use strict';
define([
    'definitions',
    'utils/Address'
], function(angular, Address) {

    var mod = angular.module('walletApp.directives');

    /**
     * This directive does not use isolated scope, so we will have access to scope.network
     */
    mod.directive('addressField', function() {
        return {
            require: 'ngModel',
            link: function(scope, elm, attrs, ctrl) {
                ctrl.$validators.addressField = function addressField(modelValue, viewValue) {
                    if (ctrl.$isEmpty(modelValue)) {
                        return false;
                    }
                    return Address.isValid(modelValue);
                };
            }
        };
    });

    mod.directive('walletNameField', function() {
        return {
            require: 'ngModel',
            link: function(scope, elm, attrs, ctrl) {
                ctrl.$validators.walletNameField = function walletNameField(modelValue, viewValue) {
                    if (ctrl.$isEmpty(modelValue)) {
                        return false;
                    }

                    var accounts = scope.$storage.wallets;
                    if (! accounts) {
                        return true;
                    }

                    // FOR now skip scope.network, and just compare all existing names
                    var elem = $.grep(scope.$storage.wallets, function(w){ return w.name === modelValue; });
                    return elem.length === 0;
                };
            }
        };
    });

    return mod;
});