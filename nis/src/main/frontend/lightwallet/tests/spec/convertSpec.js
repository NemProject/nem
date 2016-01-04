define(["utils/convert"], function (convert) {
    describe("convert utilities tests", function () {
        it("utf8ToHex encodes ascii", function() {
            expect(convert.utf8ToHex('Hello')).toEqual("48656c6c6f");
        });
        it("utf8ToHex encodes utf8", function() {
            expect(convert.utf8ToHex('Любя, съешь щипцы, - вздохнёт мэр, - кайф жгуч')).toEqual("d09bd18ed0b1d18f2c20d181d18ad0b5d188d18c20d189d0b8d0bfd186d18b2c202d20d0b2d0b7d0b4d0bed185d0bdd191d18220d0bcd18dd1802c202d20d0bad0b0d0b9d18420d0b6d0b3d183d187");
        });

        it("hex2ua does not throw on invalid input", function() {
            // toThrow requires a function, not an actual result, so wrap in bind
            expect(convert.hex2ua.bind(null, {})).not.toThrow();
        });
        it("hex2ua converts proper data", function() {
            var expected = new Uint8Array([85, 170, 144, 187]);
            expect(convert.hex2ua("55aa90bb")).toEqual(expected);
        });
        it("hex2ua discards odd bytes", function() {
            var expected = new Uint8Array([85, 170, 144]);
            expect(convert.hex2ua("55aa90b")).toEqual(expected);
        });

        it("ua2hex works on typed arrays", function() {
            var source = new Uint8Array([85, 170, 144, 187]);
            expect(convert.ua2hex(source)).toEqual("55aa90bb");
        });
        // this one is actually not a requirement...
        it("ua2hex works on untyped arrays", function() {
            var source = [85, 170, 144, 187];
            expect(convert.ua2hex(source)).toEqual("55aa90bb");
        });
        // actually maybe it'd be good if it would throw ...
        it("ua2hex does throws on invalid data", function() {
            var source = [256];
            expect(convert.ua2hex.bind(null, source)).not.toThrow();
        });

        it("roundtrip ua2hex(hex2ua())", function() {
            expect(convert.ua2hex(convert.hex2ua("55aa90bb"))).toEqual("55aa90bb");
        });
        it("roundtrip hex2ua(ua2hex())", function() {
            var source = new Uint8Array([85, 170, 144, 187]);
            expect(convert.hex2ua(convert.ua2hex(source))).toEqual(source);
        });


        it("hex2ua_reversed returns reversed array", function() {
            var expected = new Uint8Array([187, 144, 170, 85]);
            expect(convert.hex2ua_reversed("55aa90bb")).toEqual(expected);
        });
        it("hex2ua_reversed discards odd bytes", function() {
            var expected = new Uint8Array([144, 170, 85]);
            expect(convert.hex2ua_reversed("55aa90b")).toEqual(expected);
        });

        it("hex2a encodes byte-to-byte", function() {
            var source = "90909055aa90bbc3bc";
            expect(convert.hex2a(source).length).toEqual(9);
        });
    });
});