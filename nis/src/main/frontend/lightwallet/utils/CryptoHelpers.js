'use strict';

define([
    'nacl-fast',
    'utils/Address',
    'utils/KeyPair',
    'utils/convert',

    'crypto-js/pbkdf2',
    'crypto-js/aes',
], function(nacl, Address, KeyPair, convert){
    var o = {};

    o._generateKey = function(salt, password, numberOfIterations) {
        console.time('pbkdf2 generation time');
        var key256Bits = CryptoJS.PBKDF2(password, salt, { keySize: 256/32, iterations: numberOfIterations, hasher: CryptoJS.algo.SHA256 });
        console.timeEnd('pbkdf2 generation time');
        return {'salt':CryptoJS.enc.Hex.stringify(salt), 'priv':CryptoJS.enc.Hex.stringify(key256Bits)};
    };

    o.generateSaltedKey = function(password) {
        var salt = CryptoJS.lib.WordArray.random(128/8);
        return o._generateKey(salt, password, 1000);
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

    o.passwordToPrivatekeyClear = function(commonData, walletAccount, doClear) {
        if (commonData.password) {
            var r = undefined;
            if (walletAccount.algo === "pass:6k") {
                r = o.derivePassSha(commonData.password, 6000);
            } else if (walletAccount.algo === "pbkf2:1k") {
                r = o._generateKey(CryptoJS.enc.Hex.parse(walletAccount.salt), commonData.password);
            } else if (walletAccount.algo === "pass:enc") {
                var pass = o.derivePassSha(commonData.password, 20);
                var obj = {ciphertext: CryptoJS.enc.Hex.parse(walletAccount.encrypted), iv:convert.hex2ua(walletAccount.iv), key:convert.hex2ua(pass.priv)}
                var d = o.decrypt(obj);
                r = {'priv':d};
            } else {
                alert("unknown wallet encryption method");
            }
            if (doClear) {
                delete commonData.password;
            }
            return r.priv;
        } else {
            return commonData.privatekey;
        }
    }


    o.checkAddress = function(priv, network, _expectedAddress) {
        var expectedAddress = _expectedAddress.toUpperCase().replace(/-/g, '');
        var kp = KeyPair.create(priv);
        var address = Address.toAddress(kp.publicKey.toString(), network);
        return address === expectedAddress;
    };

    o.passwordToPrivatekey = function(commonData, networkId, walletAccount) {
        var priv = o.passwordToPrivatekeyClear(commonData, walletAccount, false);
        if (!o.checkAddress(priv, networkId, walletAccount.address))
        {
            return false;
        }
        commonData.privatekey = priv;
        return true;
    };

    function words2ua(destUa, cryptowords) {
        for (var i = 0; i < destUa.length; i += 4) {
            var v = cryptowords.words[i / 4];
            if (v < 0) v += 0x100000000;
            destUa[i] = (v >>> 24);
            destUa[i+1] = (v >>> 16) & 0xff;
            destUa[i+2] = (v  >>> 8) & 0xff;
            destUa[i+3] = v & 0xff;
        }
    }

    function ua2words(ua, uaLength) {
        var temp = [];
        for (var i = 0; i < uaLength; i += 4) {
            var x = ua[i]*0x1000000 + (ua[i+1] || 0)*0x10000 + (ua[i+2] || 0)* 0x100 + (ua[i+3] || 0);
            temp.push( (x > 0x7fffffff) ?  x - 0x100000000 : x );
        }
        return CryptoJS.lib.WordArray.create(temp, uaLength);
    }
    function hashfunc(dest, data, dataLength) {
        var convertedData = ua2words(data, dataLength);
        var hash = CryptoJS.SHA3(convertedData, { outputLength: 512 });
        words2ua(dest, hash);
    }
    function key_derive(shared, salt, sk, pk) {
        nacl.lowlevel.crypto_shared_key_hash(shared, pk, sk, hashfunc);
        for (var i = 0; i < salt.length; i++) {
            shared[i] ^= salt[i];
        }
        // ua2words
        var hash = CryptoJS.SHA3(ua2words(shared, 32), {
            outputLength: 256
        });
        return hash;
    }

    o.randomKey = function randomKey() {
        var rkey = new Uint8Array(32);
        window.crypto.getRandomValues(rkey);
        return rkey;
    };

    // data must be hex string
    o.encrypt = function encrypt(data, key) {
        var iv = new Uint8Array(16);
        window.crypto.getRandomValues(iv);

        var encKey = ua2words(key, 32);
        var encIv = { iv: ua2words(iv, 16) };
        var encrypted = CryptoJS.AES.encrypt(CryptoJS.enc.Hex.parse(data), encKey, encIv);
        return {ciphertext: encrypted.ciphertext, iv:iv, key:key};
    };

    // returns hex string
    o.decrypt = function decrypt(data) {
        var encKey = ua2words(data.key, 32);
        var encIv = { iv: ua2words(data.iv, 16) };
        return CryptoJS.enc.Hex.stringify(CryptoJS.AES.decrypt(data, encKey, encIv));
    };

    o.encodePrivKey = function encodePrivKey(privatekey, password) {
        var pass = o.derivePassSha(password, 20);
        var r = o.encrypt(privatekey, convert.hex2ua(pass.priv));
        var ret = {ciphertext:CryptoJS.enc.Hex.stringify(r.ciphertext), iv:convert.ua2hex(r.iv)};
        return ret;
    };

    o.encode = function(senderPriv, recipientPub, msg) {
        var iv = new Uint8Array(16);
        window.crypto.getRandomValues(iv);
        //console.log("IV:", convert.ua2hex(iv));

        var sk = convert.hex2ua_reversed(senderPriv);
        var pk = convert.hex2ua(recipientPub);
        var salt = new Uint8Array(32);
        window.crypto.getRandomValues(salt);
        //console.log("salt:", convert.ua2hex(salt));

        var shared = new Uint8Array(32);
        var r = key_derive(shared, salt, sk, pk);
        //console.log("shared 1", CryptoJS.enc.Hex.stringify(r));

        var encKey = r;
        var encIv = { iv: ua2words(iv, 16) };
        var encrypted = CryptoJS.AES.encrypt(CryptoJS.enc.Hex.parse(convert.utf8ToHex(msg)), encKey, encIv);
        var result = convert.ua2hex(salt) + convert.ua2hex(iv) + CryptoJS.enc.Hex.stringify(encrypted.ciphertext);
        //console.log("encoded", result);
        return result;
    };

    o.decode = function(recipientPrivate, senderPublic, payload) {
        var binPayload = convert.hex2ua(payload);
        var salt = new Uint8Array(binPayload.buffer, 0, 32);
        var iv = new Uint8Array(binPayload.buffer, 32, 16);
        var payload = new Uint8Array(binPayload.buffer, 48);

        var sk = convert.hex2ua_reversed(recipientPrivate);
        var pk = convert.hex2ua(senderPublic);
        var shared = new Uint8Array(32);
        var r = key_derive(shared, salt, sk, pk);

        var encKey = r;
        var encIv = { iv: ua2words(iv, 16) };

        var encrypted = {'ciphertext':ua2words(payload, payload.length)};
        var plain = CryptoJS.AES.decrypt(encrypted, encKey, encIv);
        var hexplain = CryptoJS.enc.Hex.stringify(plain);
        return hexplain;
    };
    return o;
});