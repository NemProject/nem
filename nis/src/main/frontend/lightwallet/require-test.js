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
		'crypto-js/sha3' : ['crypto-js/x64-core'],
		'crypto-js/ripemd160' : ['crypto-js/x64-core'],
		'nacl-fast': {'exports': 'nacl'}
	},
	baseUrl: '.',
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
