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
    // this is made specifically for our use, deals only with proper strings
    baseenc.b32decode = function(s) {
        var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        var r = new ArrayBuffer(s.length * 5 / 8);
        var b = new Uint8Array(r);
        for (var j = 0; j < s.length / 8; j++) {
            var v = [0,0,0,0, 0,0,0,0];
            for (var i = 0; i < 8; ++i) {
                v[i] = alphabet.indexOf(s[j*8 + i]);
            }
            var i = 0;
            b[j*5 + 0] = (v[i + 0] << 3) | (v[i + 1] >> 2);
            b[j*5 + 1] = ((v[i + 1] & 0x3) << 6) | (v[i + 2] << 1) | (v[i + 3] >> 4);
            b[j*5 + 2] = ((v[i + 3] & 0xf) << 4) | (v[i + 4] >> 1);
            b[j*5 + 3] = ((v[i + 4] & 0x1) << 7) | (v[i + 5] << 2) | (v[i + 6] >> 3);
            b[j*5 + 4] = ((v[i + 6] & 0x7) << 5) | (v[i + 7]);
        }
        return b;
    };

    var Address = {};
    Address.toAddress = function toAddress(publicKey, networkId) {
        var binPubKey = CryptoJS.enc.Hex.parse(publicKey);
        var hash = CryptoJS.SHA3(binPubKey, {
            outputLength: 256
        });
        var hash2 = CryptoJS.RIPEMD160(hash);
        // 98 is for testnet
        var networkPrefix = (networkId === -104) ? '98' : (networkId === 104 ? '68' : '60');
        var versionPrefixedRipemd160Hash = networkPrefix + CryptoJS.enc.Hex.stringify(hash2);
        var tempHash = CryptoJS.SHA3(CryptoJS.enc.Hex.parse(versionPrefixedRipemd160Hash), {
            outputLength: 256
        });
        var stepThreeChecksum = CryptoJS.enc.Hex.stringify(tempHash).substr(0, 8);
        var concatStepThreeAndStepSix = convert.hex2a(versionPrefixedRipemd160Hash + stepThreeChecksum);
        var ret = baseenc.b32encode(concatStepThreeAndStepSix);
        return ret;
    };

    Address.isValid = function isValid(_address) {
        var address = _address.toString().toUpperCase().replace(/-/g, '');
        if (!address || address.length !== 40) {
            return false;
        }
        var decoded = convert.ua2hex(baseenc.b32decode(address));
        var versionPrefixedRipemd160Hash = CryptoJS.enc.Hex.parse(decoded.slice(0, 42));
        var tempHash = CryptoJS.SHA3(versionPrefixedRipemd160Hash, {
            outputLength: 256
        });
        var stepThreeChecksum = CryptoJS.enc.Hex.stringify(tempHash).substr(0, 8);

        return stepThreeChecksum === decoded.slice(42);
    };

    return Address;
});