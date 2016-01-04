'use strict';

require.config({
    packages: [
        {
            name: 'crypto-js',
            location: 'https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.2/components/',
            main: 'index'
        }
    ],
	paths: {
		jquery: 'https://code.jquery.com/jquery-2.1.4.min',
		bootstrap: 'https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min',
		angular: 'https://ajax.googleapis.com/ajax/libs/angularjs/1.4.7/angular.min',
		angularAnimate: 'https://ajax.googleapis.com/ajax/libs/angularjs/1.4.7/angular-animate.min',
		angularRoute: 'https://ajax.googleapis.com/ajax/libs/angularjs/1.4.7/angular-route.min',
		angularSanitize: 'https://ajax.googleapis.com/ajax/libs/angularjs/1.4.7/angular-sanitize.min',
		angularUib: 'https://cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.14.3/ui-bootstrap.min',
		ngStorage: 'https://cdnjs.cloudflare.com/ajax/libs/ngStorage/0.3.9/ngStorage.min'
	},
	shim: {
		'angular' : {'exports' : 'angular'},
		'angularRoute': ['angular'],
		'angularAnimate': ['angular'],
		'angularSanitize': ['angular'],
		'angularUib': ['angular', 'bootstrap'],
		'ngStorage': ['angular'],
		'bootstrap' : ['jquery'],
		'crypto-js/core': {'exports': 'CryptoJS'},
		'crypto-js/x64-core': {'exports': 'CryptoJS.x64', deps:['crypto-js/core']},
		'crypto-js/ripemd160' : ['crypto-js/x64-core'],
		'crypto-js/sha256' : ['crypto-js/x64-core'],
		'crypto-js/sha3' : ['crypto-js/x64-core'],
		'crypto-js/hmac' : ['crypto-js/sha256'],
		'crypto-js/pbkdf2' : ['crypto-js/x64-core', 'crypto-js/hmac'],

		'crypto-js/md5' : ['crypto-js/x64-core'],
		'crypto-js/evpkdf': ['crypto-js/md5'],
		'crypto-js/cipher-core': ['crypto-js/evpkdf'],
		'crypto-js/aes' : ['crypto-js/cipher-core'],

		'nacl-fast': {'exports': 'nacl'},
		'utils/xbbcode': {'exports': 'XBBCODE'}
	},
	priority: [
		"angular"
	],
	baseUrl: '/lightwallet/',
});

require([
    'jquery',
	'angular',
	'walletApp',
	'bootstrap'
	], function($, angular, app) {
		var $html = angular.element(document.getElementsByTagName('html')[0]);
		angular.element().ready(function() {
			angular.bootstrap(document, ['walletApp']);
		});
	}
);