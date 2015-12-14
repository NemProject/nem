require.config({
    paths: {
        'jquery': 'https://code.jquery.com/jquery-2.1.4.min',
        'jasmine': 'lib/jasmine-core/jasmine',
        'jasmine-html': 'lib/jasmine-core/jasmine-html',
        'jasmine-boot': 'lib/jasmine-core/boot',
        'spec': 'spec'
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
        }
    },
    baseUrl: '../tests',
});


require(['jquery', 'jasmine-boot'], function ($, jasmine) {
    var specs = [];
    specs.push('spec/convertSpec');

    $(function () {
        require(specs, function (spec) {
            window.onload();
        });
    });

});