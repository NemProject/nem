'use strict';

define([
    'definitions',
//    'jquery',
    'utils/Address',
    'utils/convert'
], function(angular, Address, convert){
    var mod = angular.module('walletApp.filters');

    mod.filter('fmtPubToAddress', function(){
        return function fmtPubToAddress(input, networkId) {
            return input && Address.toAddress(input, networkId);
        };
    });

    mod.filter('fmtAddress', function(){
        return function fmtAddress(input){
            return input && input.toUpperCase().replace(/-/g, '').match(/.{1,6}/g).join('-');
        };
    });

    mod.filter('fmtNemDate', function(){
        var nemesis = Date.UTC(2015, 2, 29, 0, 6, 25);
        return function fmtNemDate(data){
            if (data === undefined) return data;
            var o = data;
            var t = (new Date(nemesis + o*1000));
            return t.toUTCString();
        };
    });

    function mosaicIdToName(mosaicId) {
        return mosaicId.namespaceId + ":" + mosaicId.name;
    }

    mod.filter('fmtSupply', function(){
        return function fmtSupply(data, mosaicId, mosaics) {
            if (data === undefined) return data;
            var mosaicName = mosaicIdToName(mosaicId);
            if (!(mosaicName in mosaics)) { return ['unknown mosaic divisibility', data]; }
            var mosaicDefinitionMetaDataPair = mosaics[mosaicName];
            var divisibilityProperties = $.grep(mosaicDefinitionMetaDataPair.mosaicDefinition.properties, function(w){ return w.name === "divisibility"; });
            var divisibility = divisibilityProperties.length === 1 ? ~~(divisibilityProperties[0].value) : 0;
            var o = parseInt(data, 10);
            if (! o) {
                if (divisibility === 0) {
                    return ["0", ''];
                } else {
                    return ["0", o.toFixed(divisibility).split('.')[1]];
                }
            }
            o = o / Math.pow(10, divisibility);
            var b = o.toFixed(divisibility).split('.');
            var r = b[0].split(/(?=(?:...)*$)/).join(" ");
            return  [r, b[1] || ""];
        };
    });

    mod.filter('fmtSupplyRaw', function(){
        return function fmtSupplyRaw(data, _divisibility) {
            var divisibility = ~~_divisibility;
            var o = parseInt(data, 10);
            if (! o) {
                if (divisibility === 0) {
                    return ["0", ''];
                } else {
                    return ["0", o.toFixed(divisibility).split('.')[1]];
                }
            }
            o = o / Math.pow(10, divisibility);
            var b = o.toFixed(divisibility).split('.');
            var r = b[0].split(/(?=(?:...)*$)/).join(" ");
            return  [r, b[1] || ""];
        };
    });

    mod.filter('fmtLevyFee', ['fmtSupplyFilter', function(fmtSupplyFilter) {
        return function fmtLevyFee(mosaic, multiplier, levy, mosaics) {
            if (mosaic === undefined || mosaics === undefined) return mosaic;
            if (levy === undefined || levy.type === undefined) return undefined;
            var levyValue;
            if (levy.type === 1) {
                levyValue = levy.fee;
            } else {
                levyValue = (multiplier / 1000000) * mosaic.quantity * levy.fee / 10000;
            }
            var r = fmtSupplyFilter(levyValue, levy.mosaicId, mosaics);
            return r[0] + "." + r[1];
        }
    }]);

    mod.filter('fmtNemImportanceScore', function() {
        return function fmtNemImportanceScore(data) {
            if (data === undefined) return data;
            var o = data;
            if (o) {
                o *= 10000;
                o = o.toFixed(4).split('.');
                return [o[0], o[1]];
            }
            return [o, 0];
        };
    });

    mod.filter('fmtNemValue', function() {
        return function fmtNemValue(data) {
            if (data === undefined) return data;
            var o = data;
            if (! o) {
                return ["0", '000000'];
            } else {
                o = o / 1000000;
                var b = o.toFixed(6).split('.');
                var r = b[0].split(/(?=(?:...)*$)/).join(" ");
                return  [r, b[1]];
            }
        };
    });

    mod.filter('fmtImportanceTransferMode', function(){
        return function fmtImportanceTransferMode(data) {
            if (data === undefined) return data;
            var o = data;
            if (o === 1) return "Activation";
            else if (o === 2) return "Dectivation";
            else return "Unknown";
        };
    });

    mod.filter('fmtHexToUtf8', function(){
        return function fmtHexToUtf8(data) {
            if (data === undefined) return data;
            var o = data;
            if (o && o.length > 2 && o[0]==='f' && o[1]==='e') {
                return "HEX:" + o.slice(2);
            }
            return decodeURIComponent(escape( convert.hex2a(o) ));
        };
    });

    mod.filter('fmtHexMessage', ['fmtHexToUtf8Filter', function(fmtHexToUtf8Filter){
        return function fmtHexMessage(data) {
            if (data === undefined) return data;
            if (data.type === 1) {
                return fmtHexToUtf8Filter(data.payload);
            } else {
                return '';
            }
        };
    }]);

    mod.filter('fmtSplitHex', function(){
        return function fmtSplitHex(data) {
            if (data === undefined) return data;
            var parts = data.match(/[\s\S]{1,64}/g) || [];
            var r = parts.join("\n");
            return r;
        };
    });

    mod.filter('objValues', function(){
        return function objValues(data){
            if (data === undefined) return data;
            return Object.keys(data).map(function (key) {
               return data[key];
           });
        };
    });
});