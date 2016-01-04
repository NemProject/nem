'use strict';

define([
    '/static/sockjs-0.3.4.js',
    '/static/stomp.js'
], function(){
    return function Connector(_accountAddress) {
        return {
            originalAddress: _accountAddress,
            // this is important, we need upper case when subscribing
            accountAddress: _accountAddress.replace(/-/g, "").toUpperCase(),
            socket: undefined,
            stompClient: undefined,
            timeoutHandle: undefined,
            alreadyForced: false,

            subscribeToMultisig: function(address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) {
                    return false;
                }
                self.stompClient.send("/w/api/account/subscribe", {}, "{'account':'"+address+"'}");
                return true;
            },

            requestAccountData: function(_address) {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestAccountData(_address); }, 100);
                } else {
                    // triggers sending of initial account state
                    // mind that we need to pass a STRING not an object
                    var address = _address || self.accountAddress;
                    self.stompClient.send("/w/api/account/get", {}, "{'account':'"+address+"'}");
                }
            },
            requestAccountTransactions: function(_address) {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestAccountTransactions(_address); }, 100);
                } else {
                    // triggers sending of most recent transfers
                    var address = _address || self.accountAddress;
                    self.stompClient.send("/w/api/account/transfers/all", {}, "{'account':'"+address+"'}");
                }
            },
            requestAccountMosaicDefinitions: function(_address) {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestAccountMosaicDefinitions(_address); }, 100);
                } else {
                    // triggers sending of most recent transfers
                    var address = _address || self.accountAddress;
                    self.stompClient.send("/w/api/account/mosaic/owned/definition", {}, "{'account':'"+address+"'}");
                }
            },
            requestAccountMosaics: function(_address) {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestAccountMosaics(_address); }, 100);
                } else {
                    // triggers sending of most recent transfers
                    var address = _address || self.accountAddress;
                    self.stompClient.send("/w/api/account/mosaic/owned", {}, "{'account':'"+address+"'}");
                }
            },
            requestAccountNamespaces: function(_address) {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestAccountNamespaces(_address); }, 100);
                } else {
                    // triggers sending of most recent transfers
                    var address = _address || self.accountAddress;
                    self.stompClient.send("/w/api/account/namespace/owned", {}, "{'account':'"+address+"'}");
                }
            },


            on: function(name, cb) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                switch(name) {
                    case 'errors':
                        self.stompClient.subscribe('/errors', function(data){
                            var error = JSON.parse(data.body);
                            cb('errors', error);
                        });
                        break;
                    case 'newblocks':
                        self.stompClient.subscribe('/blocks/new', function(data){
                            var blockHeight = JSON.parse(data.body);
                            cb(blockHeight);
                        });
                        break;
                    case 'account':
                        self.stompClient.subscribe('/account/' + self.accountAddress, function(data){
                            cb(JSON.parse(data.body));
                        });
                        break;
                    case 'recenttransactions':
                        self.stompClient.subscribe('/recenttransactions/' + self.accountAddress, function(data){
                            cb(JSON.parse(data.body));
                        });
                        break;
                    default:
                        throw "Invalid argument";
                }
                return true;
            },

            onUnconfirmed: function(cbUnconfirmed, _address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                var address = _address || self.accountAddress;
                self.stompClient.subscribe('/unconfirmed/' + address, function(data){
                    cbUnconfirmed(JSON.parse(data.body));
                });
                return true;
            },

            onConfirmed: function(cbConfirmed, _address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                // we could have subscribed only to /unconfirmed/
                // but then in case of multisig txes, we wouldn't have any indication if it got included or not
                var address = _address || self.accountAddress;
                self.stompClient.subscribe('/transactions/' + address, function(data){
                    cbConfirmed(JSON.parse(data.body));
                });
                return true;
            },

            onMosaicDefinition: function(cbMosaic, _address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                var address = _address || self.accountAddress;
                self.stompClient.subscribe('/account/mosaic/owned/definition/' + address, function(data) {
                    cbMosaic(JSON.parse(data.body));
                });
            },

            onMosaic: function(cbMosaic, _address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                var address = _address || self.accountAddress;
                self.stompClient.subscribe('/account/mosaic/owned/' + address, function(data) {
                    cbMosaic(JSON.parse(data.body), address);
                });
            },

            onNamespace: function(cbNamespace, _address) {
                var self = this;
                if (self.socket.readyState !== SockJS.OPEN) { return false; }

                var address = _address || self.accountAddress;
                self.stompClient.subscribe('/account/namespace/owned/' + address, function(data) {
                    cbNamespace(JSON.parse(data.body), address);
                });
            },

            close: function() {
                var self = this;
                self.socket.close();
            },

            connect: function(asyncConnectCb) {
                var self = this;
                self.socket = new SockJS('/w/messages');
                self.stompClient = Stomp.over(self.socket);
                self.stompClient.debug = undefined;
                self.stompClient.connect({}, function(frame) {
                    if (undefined !== asyncConnectCb) {
                        asyncConnectCb();
                    }
                }, function() {
                    // this will reconnect on failure, but will keep trying even when it shouldn't (e.g. server dies)
                    clearTimeout(self.timeoutHandle);
                    self.connect();
                });
            }
        };
    };
});