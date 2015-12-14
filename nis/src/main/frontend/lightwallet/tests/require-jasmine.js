require.config({
    packages: [
        {
            name: 'crypto-js',
            location: 'https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.2/components/',
            main: 'index'
        }
    ],
    paths: {
        'jquery': 'https://code.jquery.com/jquery-2.1.4.min',
        'jasmine': 'lib/jasmine-core/jasmine',
        'jasmine-html': 'lib/jasmine-core/jasmine-html',
        'jasmine-boot': 'lib/jasmine-core/boot',
        'spec': 'spec',
        // not very nice, but we need it so that other imports will work correctly
        'utils': '../utils'
    },
    shim: {
        'jasmine': {
            exports: 'window.jasmineRequire'
        },
        'jasmine-html': {
            deps: ['jasmine'],
            exports: 'window.jasmineRequire'
        },
        'jasmine-boot': {
            deps: ['jasmine', 'jasmine-html'],
            exports: 'window.jasmineRequire'
        },

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
    },
    baseUrl: '../tests',
});


require(['jquery', 'jasmine-boot'], function ($, jasmine) {
    var specs = [];
    specs.push('spec/convertSpec');
    specs.push('spec/AddressSpec');

    $(function () {
        require(specs, function (spec) {
            window.onload();
        });
    });

});