'use strict';

define([
    'angular',
    'angularRoute',
    'angularAnimate',
    'angularSanitize',
    'angularUib',
    'ngStorage'
], function(angular, angularRoute) {
    angular.module('walletApp.services', []);
    angular.module('walletApp.directives', []);
    angular.module('walletApp.filters', []);
    angular.module('walletApp.controllers', ['ngRoute', 'ui.bootstrap', 'ngStorage', 'ngAnimate', 'ngSanitize', 'walletApp.filters']);
    angular.module('walletApp', ['ngRoute', 'walletApp.controllers', 'walletApp.services', 'walletApp.directives']);
    return angular;
});