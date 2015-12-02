'use strict';

define([
    'angular',
    'angularRoute',
    'angularAnimate',
    'angularUib',
    'ngStorage'
], function(angular, angularRoute) {
    angular.module('walletApp.services', []);
    angular.module('walletApp.filters', []);
    angular.module('walletApp.controllers', ['ngRoute', 'ui.bootstrap', 'ngStorage', 'ngAnimate', 'walletApp.filters']);
    angular.module('walletApp', ['ngRoute', 'walletApp.controllers', 'walletApp.services']);
    return angular;
});