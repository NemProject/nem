define(["utils/Address", "utils/convert"], function (Address, convert) {
    describe("Address tests", function () {
        function generateRandomKey() {
            var rawPublicKey = new Uint8Array(32);
            window.crypto.getRandomValues(rawPublicKey);
            return convert.ua2hex(rawPublicKey);
        }
        it("Can create mainnet address", function(){
            var publicKey = generateRandomKey();
            var address = Address.toAddress(publicKey, 104);
            expect(address[0]).toEqual('N');
            expect(address.length).toBe(40);
        });
        it("Can create testnet address", function(){
            var publicKey = generateRandomKey();
            var address = Address.toAddress(publicKey, -104);
            expect(address[0]).toEqual('T');
            expect(address.length).toBe(40);
        });

        it("Same public key yields same address", function(){
            var publicKey = generateRandomKey();
            var address1 = Address.toAddress(publicKey, -104);
            var address2 = Address.toAddress(publicKey, -104);
            expect(address1).toEqual(address2);
            expect(address1.length).toBe(40);
        });

        it("Different network yields different address", function(){
            var publicKey = generateRandomKey();
            var address1 = Address.toAddress(publicKey, -104);
            var address2 = Address.toAddress(publicKey, 104);
            expect(address1.slice(1)).not.toEqual(address2.slice(1));
            expect(address1.length).toBe(40);
        });

        it("Generated address is valid", function(){
            var publicKey = generateRandomKey();
            var address = Address.toAddress(publicKey, 104);
            expect(Address.isValid(address)).toBe(true);
            expect(address.length).toBe(40);
        });

        it("Altered address is not valid", function(){
            var publicKey = generateRandomKey();
            var address = Address.toAddress(publicKey, 104);
            var modifiedAddress = 'T' + address.slice(1);
            expect(Address.isValid(address)).toBe(true);
            expect(Address.isValid(modifiedAddress)).toBe(false);
        });
    });
});