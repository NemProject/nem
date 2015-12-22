'use strict';

define([
    'definitions',
], function(angular) {
    angular.module('walletApp.services').service('sessionData', [function() {
        var networkId = undefined;
        var nisPort = 0;
        var rememberedKey = undefined;
        return {
            setNetworkId: function setNetworkId(id) {
                networkId = id;
            },
            getNetworkId: function getNetworkId() {
                return networkId;
            },
            setNisPort: function setNisPort(port) {
                nisPort = port;
            },
            getNisPort: function getNisPort() {
                return nisPort;
            },
            setRememberedKey: function setRememberedKey(data) {
                rememberedKey = data
            },
            getRememberedKey: function getRememberedKey() {
                return rememberedKey;
            }
        };
    }])
});