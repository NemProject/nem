'use strict';

define([
    '/static/sockjs-0.3.4.js',
    '/static/stomp.js'
], function(){
    return function NodeConnector() {
        return {
            socket: undefined,
            stompClient: undefined,
            timeoutHandle: undefined,

            requestNodeInfo: function() {
                var self = this;
                // if not ready, wait a bit more...
                if (self.socket.readyState !== SockJS.OPEN) {
                    self.timeoutHandle = setTimeout(function () { self.requestNodeInfo(_address); }, 100);
                } else {
                    self.stompClient.send("/w/api/node/info");
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
                    case 'nodeInfo':
                        self.stompClient.subscribe('/node/info', function(data){
                            cb(JSON.parse(data.body));
                        });
                        break
                    default:
                        throw "Invalid argument";
                }
                return true;
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