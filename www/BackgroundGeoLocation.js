var exec = require("cordova/exec");
module.exports = {
    /**
    * @property {Object} stationaryRegion
    */
    stationaryRegion: null,
    /**
    * @property {Object} config
    */
    config: {},

    /**
     * Called when the background is entered
     */
    onruninbackground : function () {},

    /**
     * Called when the foreground is entered
     */
    onruninforeground : function () {},

    /**
     * Called when location service is activated
     */
    onactivate : function () {},

    /**
     * Called when location service is deactivated
     */
    ondeactivate : function () {},

    /**
     * Called when location service is enabled (service is allowed to start)
     */
    onenable : function () {},

    /**
     * Called when location service is disabled (service is not allowed to start)
     */
    ondisable : function () {},

    /**
     * Called when the background mode has been deaktivated.
     */
    onmessage : function () {},

    
    /**
     * Called for android if new location was found
     */
    callbackFn : function (location) {},

    configure: function(success, failure, config) {
        this.config = config;
        var params              = JSON.stringify(config.params || {}),
            headers		        = JSON.stringify(config.headers || {}),
            url                 = config.url        || 'BackgroundGeoLocation_url',
            stationaryRadius    = (config.stationaryRadius >= 0) ? config.stationaryRadius : 50,    // meters
            distanceFilter      = (config.distanceFilter >= 0) ? config.distanceFilter : 500,       // meters
            locationTimeout     = (config.locationTimeout >= 0) ? config.locationTimeout : 60,      // seconds
            desiredAccuracy     = (config.desiredAccuracy >= 0) ? config.desiredAccuracy : 100,     // meters
            debug               = config.debug || false,
            notificationTitle   = config.notificationTitle || "Background tracking",
            notificationText    = config.notificationText || "ENABLED";
            activityType        = config.activityType || "OTHER";
            stopOnTerminate     = config.stopOnTerminate || false;

        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'configure',
             [params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
        );
    },
    start: function(success, failure, config) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'start',
             []);
    },
    stop: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'stop',
            []);
    },
    enable: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'enable',
            []);
    },
    disable: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'disable',
            []);
    },

    finish: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'finish',
            []);
    },
    changePace: function(isMoving, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'onPaceChange',
            [isMoving]);
    },
    /**
    * @param {Integer} stationaryRadius
    * @param {Integer} desiredAccuracy
    * @param {Integer} distanceFilter
    * @param {Integer} timeout
    */
    setConfig: function(success, failure, config) {
        this.apply(this.config, config);
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setConfig',
            [config]);
    },
    /**
    * Returns current stationaryLocation if available.  null if not
    */
    getStationaryLocation: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'getStationaryLocation',
            []);
    },
    /**
    * Add a stationary-region listener.  Whenever the devices enters "stationary-mode", your #success callback will be executed with #location param containing #radius of region
    * @param {Function} success
    * @param {Function} failure [optional] NOT IMPLEMENTED
    */
    onStationary: function(success, failure) {
        var me = this;
        success = success || function() {};
        var callback = function(region) {
            me.stationaryRegion = region;
            success.apply(me, arguments);
        };
        exec(callback,
            failure || function() {},
            'BackgroundGeoLocation',
            'addStationaryRegionListener',
            []);
    },
    apply: function(destination, source) {
        source = source || {};
        for (var property in source) {
            if (source.hasOwnProperty(property)) {
                destination[property] = source[property];
            }
        }
        return destination;
    }
};
