'use strict';

define([
    'utils/convert',
    'crypto-js/sha3',
    'crypto-js/ripemd160',
], function(convert){
    var baseenc = baseenc || {};
    baseenc.b32encode = function(s) {
        /* encodes a string s to base32 and returns the encoded string */
        var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        var parts = [];
        var quanta= Math.floor((s.length / 5));
        var leftover = s.length % 5;

        if (leftover != 0) {
           for (var i = 0; i < (5-leftover); i++) { s += '\x00'; }
           quanta += 1;
        }

        for (i = 0; i < quanta; i++) {
           parts.push(alphabet.charAt(s.charCodeAt(i*5) >> 3));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5) & 0x07) << 2)
               | (s.charCodeAt(i*5+1) >> 6)));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+1) & 0x3F) >> 1) ));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+1) & 0x01) << 4)
               | (s.charCodeAt(i*5+2) >> 4)));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+2) & 0x0F) << 1)
               | (s.charCodeAt(i*5+3) >> 7)));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+3) & 0x7F) >> 2)));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+3) & 0x03) << 3)
               | (s.charCodeAt(i*5+4) >> 5)));
           parts.push(alphabet.charAt( ((s.charCodeAt(i*5+4) & 0x1F) )));
        }

        var replace = 0;
        if (leftover == 1) replace = 6;
        else if (leftover == 2) replace = 4;
        else if (leftover == 3) replace = 3;
        else if (leftover == 4) replace = 1;

        for (i = 0; i < replace; i++) parts.pop();
        for (i = 0; i < replace; i++) parts.push("=");

        return parts.join("");
    };

    function toAddress(publicKey) {
        var binPubKey = CryptoJS.enc.Hex.parse(publicKey);
        var hash = CryptoJS.SHA3(binPubKey, {
            outputLength: 256
        });
        var hash2 = CryptoJS.RIPEMD160(hash);
        // 98 is for testnet
        var versionPrefixedRipemd160Hash = '98' + CryptoJS.enc.Hex.stringify(hash2);
        var tempHash = CryptoJS.SHA3(CryptoJS.enc.Hex.parse(versionPrefixedRipemd160Hash), {
            outputLength: 256
        });
        var stepThreeChecksum = CryptoJS.enc.Hex.stringify(tempHash).substr(0, 8);;
        var concatStepThreeAndStepSix = convert.hex2a(versionPrefixedRipemd160Hash + stepThreeChecksum);
        var ret = baseenc.b32encode(concatStepThreeAndStepSix);
        return ret;
    }

    return toAddress;
});