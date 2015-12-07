'use strict';

define([
    'nacl-fast',

    'crypto-js/pbkdf2',
], function(nacl){
    var o = {};

    o._generateKey = function(salt, password) {
        console.time('pbkdf2 generation time');
        var key256Bits = CryptoJS.PBKDF2(password, salt, { keySize: 256/32, iterations: 1000, hasher: CryptoJS.algo.SHA256 });
        console.timeEnd('pbkdf2 generation time');
        return {'salt':CryptoJS.enc.Hex.stringify(salt), 'priv':CryptoJS.enc.Hex.stringify(key256Bits)};
    };

    o.generateSaltedKey = function(password) {
        var salt = CryptoJS.lib.WordArray.random(128/8);
        return o._generateKey(salt, password);
    };

    o.derivePassSha = function(password, count) {
        var data = password;
        console.time('sha3^n generation time');
        for (var i = 0; i < count; ++i) {
            data = CryptoJS.SHA3(data, {
                outputLength: 256
            });
        }
        console.timeEnd('sha3^n generation time');
        var r = {'priv':CryptoJS.enc.Hex.stringify(data)};
        return r;
    };

    o.passwordToPrivatekeyClear = function(txdata, walletAccount, doClear) {
        if (txdata.password) {
            var r = undefined;
            if (walletAccount.algo === "pass:6k") {
                r = o.derivePassSha(txdata.password, 6000);
            } else if (walletAccount.algo === "pbkf2:1k") {
                r = o._generateKey(CryptoJS.enc.Hex.parse(walletAccount.salt), txdata.password);
            }
            if (doClear) {
                delete txdata.password;
            }
            return r.priv;
        } else {
            return txdata.privatekey;
        }
    }

    o.passwordToPrivatekey = function(txdata, walletAccount) {
        var priv = o.passwordToPrivatekeyClear(txdata, walletAccount, false);
        txdata.privatekey = priv;
    }

    return o;
});