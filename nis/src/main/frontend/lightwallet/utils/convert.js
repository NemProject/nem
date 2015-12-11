'use strict';

define([],function(){
    var _hexEncodeArray = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];

    var o = {};
    o.hex2ua_reversed = function hex2ua_reversed(hexx) {
        var hex = hexx.toString();//force conversion
        var ua = new Uint8Array(hex.length / 2);
        for (var i = 0; i < hex.length; i += 2) {
            ua[ua.length - 1 - (i / 2)] = parseInt(hex.substr(i, 2), 16);
        }
        return ua;
    };

    o.hex2ua = function hex2ua(hexx) {
        var hex = hexx.toString();//force conversion
        var ua = new Uint8Array(hex.length / 2);
        for (var i = 0; i < hex.length; i += 2) {
            ua[i / 2] = parseInt(hex.substr(i, 2), 16);
        }
        return ua;
    };

    o.ua2hex = function ua2hex(ua) {
        var s = '';
        for (var i = 0; i < ua.length; i++) {
            var code = ua[i];
            s += _hexEncodeArray[code >>> 4];
            s += _hexEncodeArray[code & 0x0F];
        }
        return s;
    };

    o.hex2a = function hex2a(hexx) {
        var hex = hexx.toString();
        var str = '';
        for (var i = 0; i < hex.length; i += 2)
            str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
        return str;
    };

    o.utf8ToHex = function utf8ToHex(str) {
        var hex;
        try {
            hex = unescape(encodeURIComponent(str)).split('').map(function(v){
                return v.charCodeAt(0).toString(16)
            }).join('');
        } catch(e){
            hex = str;
            console.log('invalid text input: ' + str);
        }
        return hex;
    };

    return o;
});