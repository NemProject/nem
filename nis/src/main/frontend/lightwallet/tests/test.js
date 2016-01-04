'use strict';

define([
	'jquery',
	'../nacl-fast',
	'../utils/convert',
	'0.test-sha3-256',
	'1.test-keys',
	'2.test-sign',
	'3.test-derive',
	'4.test-cipher',
    'crypto-js/sha3',
    'crypto-js/ripemd160',
    'crypto-js/pbkdf2',
    'crypto-js/aes',
], function($, nacl, convert, testsha256, testkeys, testsign, testderive, testcipher) {
	function assert(condition, message) {
		if (!condition) {
			message = message || "Assertion failed";
			if (typeof Error !== "undefined") {
				throw new Error(message);
			}
			throw message; // Fallback
		}
	}

	function gf(init) {
		var i, r = new Float64Array(16);
		if (init) for (i = 0; i < init.length; i++) r[i] = init[i];
		return r;
	}

	function ua2words(ua, uaLength) {
		var temp = [];
		for (var i = 0; i < uaLength; i += 4) {
			var x = ua[i]*0x1000000 + ua[i+1]*0x10000 + ua[i+2]* 0x100 + ua[i+3];
			temp.push( (x > 0x7fffffff) ?  x - 0x100000000 : x );
		}
		return CryptoJS.lib.WordArray.create(temp, uaLength);
	}
	function words2ua(destUa, cryptowords)
	{
		for (var i = 0; i < destUa.length; i += 4) {
			var v = cryptowords.words[i / 4];
			if (v < 0) v += 0x100000000;
			destUa[i] = (v >>> 24);
			destUa[i+1] = (v >>> 16) & 0xff;
			destUa[i+2] = (v  >>> 8) & 0xff;
			destUa[i+3] = v & 0xff;
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
			if (data instanceof Uint8Array) {
				var converted = ua2words(data, data.length);
				var result = CryptoJS.enc.Hex.stringify(converted);
				this.sha3.update(converted);

			} else if (typeof data === "string") {
				var converted = CryptoJS.enc.Hex.parse(data);
				this.sha3.update(converted);

			} else {
				assert(false, "unhandled argument");
			}
		}
		this.finalize = function(result) {
			var hash = this.sha3.finalize();
			words2ua(result, hash);
		};
	}
	
	function pk2pub(hexdata) {
		var pk = new Uint8Array(nacl.lowlevel.crypto_sign_PUBLICKEYBYTES);
		var sk = convert.hex2ua(hexdata);
		nacl.lowlevel.crypto_sign_keypair_hash(pk, sk, hashfunc);
		return {publicKey: pk, secretKey: sk};
	}

	function pk2pub_reversed(hexdata) {
		var pk = new Uint8Array(nacl.lowlevel.crypto_sign_PUBLICKEYBYTES);
		var sk = convert.hex2ua_reversed(hexdata);
		nacl.lowlevel.crypto_sign_keypair_hash(pk, sk, hashfunc);
		return {publicKey: pk, secretKey: sk};
	}
	
	function sign_reversed(sig, key, data) {
		var keyPair = pk2pub_reversed(key);
		var hasher = new hashobj();
		var r = nacl.lowlevel.crypto_sign_hash(sig, keyPair, data, hasher);
		return r;
	}

	function key_derive(shared, salt, sk, pk) {
		nacl.lowlevel.crypto_shared_key_hash(shared, pk, sk, hashfunc);
        var temp = new Uint8Array(salt.length);
        for (var i = 0; i < salt.length; i++) {
            temp[i] = shared[i] ^ salt[i];
        }
        // ua2words
        var hash = CryptoJS.SHA3(ua2words(temp, 32), {
            outputLength: 256
        });
        return [shared, CryptoJS.enc.Hex.stringify(hash)];
	}

	return function fun() {
		(function test1(){
			var data = "c5247738c3a510fb6c11413331d8a47764f6e78ffcdb02b6878d5dd3b77f38ed";
			var expected = "70c9dcf696b2ad92dbb9b52ceb33ec0eda5bfdb7052df4914c0919caddb9dfcf";
			var binPubKey = CryptoJS.enc.Hex.parse(data);
			var hash = CryptoJS.SHA3(binPubKey, {
				outputLength: 256
			});
			var result = CryptoJS.enc.Hex.stringify(hash);
			assert(result === expected, "sha3selfTest failed")
			console.log(expected, result, expected === result);
		})();
		(function test2(){
			var data = "f18efd042af93b0ee124a20b739571b0ed66e76ae3a111f00297db305ebae7b2";
			var expected = "86292f63fc79c34f2af7392e27f0795fcdce6378251555c741b57f2be708df2850a9b6a3a2d92cb4ac80dc64d7bdc8d43890c634628751ef9ef9636ef8bfaf4e";
			var binPubKey = CryptoJS.enc.Hex.parse(data);
			var hash = CryptoJS.SHA3(binPubKey, {
				outputLength: 512
			});
			var result = CryptoJS.enc.Hex.stringify(hash);
			assert(result === expected, "sha3-512selfTest failed")
			console.log(expected, result, expected === result);
		})();
		(function test3(){
			var data = "70c9dcf696b2ad92dbb9b52ceb33ec0eda5bfdb7052df4914c0919caddb9dfcf";
			var expected = "1f142c5ea4853063ed6dc3c13aaa8257cd7daf11";
			var binPubKey = CryptoJS.enc.Hex.parse(data);
			var hash = CryptoJS.RIPEMD160(binPubKey);
			var result = CryptoJS.enc.Hex.stringify(hash);
			assert(result === expected, "ripemd160selfTest failed");
			console.log(expected, result, expected === result);
		})();
		(function test4(){
			var data = "f18efd042af93b0ee124a20b739571b0ed66e76ae3a111f00297db305ebae7b2";
			var expected = "c5247738c3a510fb6c11413331d8a47764f6e78ffcdb02b6878d5dd3b77f38ed";
			var keys = pk2pub(data);
			var result = convert.ua2hex(keys.publicKey);
			assert (result === expected, "keypairSelfTest failed");
			console.log(expected, result, expected === result);
		})();
		(function testSha(){
		    if (testsha256.length) console.log("running ("+testsha256.length+") sha3-256 tests");
			for (var elem of testsha256)
			{
				var d = CryptoJS.enc.Hex.parse(elem.data);
				var hash = CryptoJS.SHA3(d, {
					outputLength: 256
				});
				var result = CryptoJS.enc.Hex.stringify(hash);
				assert(result === elem.sha);
			}
			if (testsha256.length) console.log("PASSED");
		})();
		(function testKeys(){
		    if (testkeys.length) console.log("running ("+testkeys.length+") key generation tests");
			for (var elem of testkeys)
			{
				var keys = pk2pub_reversed(elem.priv);
				var result = convert.ua2hex(keys.publicKey);
				assert(result === elem.pub);
			}
			if (testkeys.length) console.log("PASSED");
		})();
		(function testSign(){
		    if (testsign.length) console.log("running ("+testsign.length+") signing tests");
			for (var elem of testsign)
			{
				var sig = new Uint8Array(64);
				var keys = pk2pub_reversed(elem.priv);
				var result = convert.ua2hex(keys.publicKey);

				var r = sign_reversed(sig, elem.priv, elem.data);
				assert(result === elem.pub);
				assert(r);
				assert(convert.ua2hex(sig) === elem.sig);
			}
			if (testsign.length) console.log("PASSED");
		})();
		(function testDerive(){
		    if (testderive.length) console.log("running ("+testderive.length+") key deriviation tests");
			for (var elem of testderive) {
			    var sk = convert.hex2ua_reversed(elem.priv);
                var pk = convert.hex2ua(elem.pub);
                var salt = convert.hex2ua(elem.salt);

				var shared = new Uint8Array(32);
				var r = key_derive(shared, salt, sk, pk);
				//console.log("elem.mul", elem.mul, convert.ua2hex(r[0]));
				//console.log(elem.shared);
				assert(convert.ua2hex(r[0]) === elem.mul);
				assert(r[1] === elem.shared);
			}
			if (testderive.length) console.log("PASSED");
		})();
		(function testCipher(){
		    if (testcipher.length) console.log("running ("+testcipher.length+") encrypt tests");
		    for (var elem of testcipher) {
                var sk = convert.hex2ua_reversed(elem.priv);
                var pk = convert.hex2ua(elem.pub);
                var salt = convert.hex2ua(elem.salt);

                var shared = new Uint8Array(32);
                var r = key_derive(shared, salt, sk, pk);
                //console.log(r[1]);

                var encKey = CryptoJS.enc.Hex.parse(r[1]);
                var encIv = { iv: CryptoJS.enc.Hex.parse(elem.iv) };
                var encrypted = CryptoJS.AES.encrypt(CryptoJS.enc.Hex.parse(elem.input), encKey, encIv);
                assert(CryptoJS.enc.Hex.stringify(encrypted.ciphertext) === elem.out)
		    }
		    if (testcipher.length) console.log("PASSED");
		})();
	};
});
