'use strict';

define([
    'nacl-fast',
    'utils/convert',

    'crypto-js/sha3',
], function(nacl, convert){
    function ua2words(ua, uaLength) {
        var temp = [];
        for (var i = 0; i < uaLength; i += 4) {
            var x = ua[i]*0x1000000 + (ua[i+1] || 0)*0x10000 + (ua[i+2] || 0)* 0x100 + (ua[i+3] || 0);
            temp.push( (x > 0x7fffffff) ?  x - 0x100000000 : x );
        }
        return CryptoJS.lib.WordArray.create(temp, uaLength);
    }
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
    function BinaryKey(keyData) {
        this.data = keyData;
        this.toString = function() {
            return convert.ua2hex(this.data);
        }
    }
    function hashfunc(dest, data, dataLength) {
        var convertedData = ua2words(data, dataLength);
        var hash = CryptoJS.SHA3(convertedData, { outputLength: 512 });
        words2ua(dest, hash);
    }
    function hashobj() {
        this.sha3 = CryptoJS.algo.SHA3.create({ outputLength: 512 });
        this.reset = function() {
            this.sha3 = CryptoJS.algo.SHA3.create({ outputLength: 512 });
        }
        this.update = function(data) {
            if (data instanceof BinaryKey) {
                var converted = ua2words(data.data, data.data.length);
                var result = CryptoJS.enc.Hex.stringify(converted);
                this.sha3.update(converted);

            } else if (data instanceof Uint8Array) {
                var converted = ua2words(data, data.length);
                this.sha3.update(converted);

            } else if (typeof data === "string") {
                var converted = CryptoJS.enc.Hex.parse(data);
                this.sha3.update(converted);

            } else {
                throw new Error("unhandled argument");
            }
        }
        this.finalize = function(result) {
            var hash = this.sha3.finalize();
            words2ua(result, hash);
        };
    }

    function KeyPair(privkey) {
        this.publicKey = new BinaryKey(new Uint8Array(nacl.lowlevel.crypto_sign_PUBLICKEYBYTES));
        this.secretKey = convert.hex2ua_reversed(privkey);
        nacl.lowlevel.crypto_sign_keypair_hash(this.publicKey.data, this.secretKey, hashfunc);

        this.sign = function(data) {
            var sig = new Uint8Array(64);
            var hasher = new hashobj();
            var r = nacl.lowlevel.crypto_sign_hash(sig, this, data, hasher);
            if (!r) {
                alert("couldn't sign the tx, generated invalid signature");
                throw new Error("couldn't sign the tx, generated invalid signature");
            }
            return new BinaryKey(sig);
        }
    };
    var o = {
        create: function(hexdata) {
            var r = new KeyPair(hexdata);
            return r;
        }
    };
    return o;
});