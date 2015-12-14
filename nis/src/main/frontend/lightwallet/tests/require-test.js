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
	},
	shim: {
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

		'../nacl-fast': {'exports': 'nacl'}
	},
	baseUrl: '../tests',
});

require([
    'jquery',
	'bootstrap',
	'test'
	], function($, _, t) {
		console.log('systems ready...');
		t();
	}
);
