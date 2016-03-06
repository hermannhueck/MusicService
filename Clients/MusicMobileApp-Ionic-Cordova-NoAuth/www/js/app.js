'use strict';

angular.module('MusicApp', ['ionic', 'ngCordova', 'ngResource', 'MusicApp.controllers', 'MusicApp.services'])


    .run(function($rootScope, $ionicPlatform, $ionicLoading, $cordovaSplashscreen, $timeout) {
    
    
        $ionicPlatform.ready(function() {

            console.log("Ionic Platform is ready.");

            console.log("Current Device is: " + JSON.stringify(ionic.Platform.device()));
            console.log("Current Platform is: " + ionic.Platform.platform());
            console.log("Using a WebView? " + ionic.Platform.isWebView());
            console.log("cordova available? " + (typeof window.cordova !== 'undefined'));

            // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
            // for form inputs)
            if (window.cordova && window.cordova.plugins && window.cordova.plugins.Keyboard) {
                cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
                cordova.plugins.Keyboard.disableScroll(true);
            }

            if (window.StatusBar) {
                // org.apache.cordova.statusbar required
                StatusBar.styleDefault();
            }

            $timeout(function() {
                if (window.cordova) {
                    $cordovaSplashscreen.hide();
                }
            }, 20000);
        });

    
        $rootScope.$on('loading:show', function () {
            // console.log("On loading:show ...");
            $ionicLoading.show({
                template: '<ion-spinner></ion-spinner> Loading ...'
            });
        });

        $rootScope.$on('loading:hide', function () {
            // console.log("On loading:hide");
            $ionicLoading.hide();
        });

        // show Loading when a state change begins
        $rootScope.$on('$stateChangeStart', function () {
            console.log('On $stateChangeStart: Start Loading');
            $rootScope.$broadcast('loading:show');
        });

        // hide Loading when a state change completes successfully
        $rootScope.$on('$stateChangeSuccess', function () {
            console.log('On $stateChangeSuccess: Hide Loading');
            $rootScope.$broadcast('loading:hide');
        });

        // hide Loading when a state change completes with an error
        $rootScope.$on('$stateChangeError', function () {
            console.log('On $stateChangeError: Hide Loading');
            $rootScope.$broadcast('loading:hide');
        });

        // just log when a state is not found
        $rootScope.$on('$stateNotFound', function () {
            console.log('On $stateNotFound');
        });
    
    })


    .config(function($stateProvider, $urlRouterProvider, $httpProvider, $sceDelegateProvider) {
    
        $stateProvider
        
            .state('app', {
                url: '/app',
                abstract: true,
                templateUrl: 'templates/sidebar.html',
                controller: 'AppController'
            })
 
            .state('app.home', {
                url: '/home',
                views: {
                    'mainContent': {
                        templateUrl: 'templates/home.html',
                        controller: 'HomeController'
                    }
                }
            })

            .state('app.contactus', {
                url: '/contactus',
                views: {
                    'mainContent': {
                        templateUrl: 'templates/contactus.html',
                        controller: 'ContactController'
                    }
                }
            })

            .state('app.settings', {
                url: '/settings',
                views: {
                    'mainContent': {
                        templateUrl: 'templates/settings.html',
                        controller: 'SettingsController'
                    }
                }
            })

            .state('app.performers', {
                url:'/performers',
                views: {
                    'mainContent': {
                        templateUrl : 'templates/performers.html',
                        controller  : 'PerformersController'
                   }
                }
            })
            
            .state('app.recordings', {
                url:'/recordings',
                views: {
                    'mainContent': {
                        templateUrl : 'templates/recordings.html',
                        controller  : 'RecordingsController'
                     }
                }
            })

            .state('app.performerDetails', {
                url: '/performers/:id',
                views: {
                    'mainContent': {
                        templateUrl : 'templates/performer-details.html',
                        controller  : 'PerformerDetailsController'
                    }
                }
            })

            .state('app.recordingDetails', {
                url: '/recordings/:id',
                views: {
                    'mainContent': {
                        templateUrl : 'templates/recording-details.html',
                        controller  : 'RecordingDetailsController'
                   }
                }
            });
            
            // default route
            $urlRouterProvider.otherwise('/app/home');
            //$urlRouterProvider.otherwise('/app/performers');
            //$urlRouterProvider.otherwise('/app/recordings');

/*
            $httpProvider.defaults.useXDomain = true;
            delete $httpProvider.defaults.headers.common['X-Requested-With'];
            $sceDelegateProvider.resourceUrlWhitelist(['self',
                                                       'http://localhost:9000/**'
                                                      ]);
*/
    })

;
