'use strict';

angular.module('musicSPA', ['ui.router', 'ngResource', 'angularSpinners', 'musicSPA.controllers', 'musicSPA.services'])


    .run(function($rootScope, spinnerService) {
    
        
        $rootScope.$on('loading:show', function () {
            console.log("On loading:show ...");
            spinnerService.show('loadingSpinner');
        });

        $rootScope.$on('loading:hide', function () {
            console.log("On loading:hide");
            spinnerService.hide('loadingSpinner');
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
                url:'/',
                views: {
                    'header': {
                        templateUrl : 'views/header.html',
                    },
                    'content': {
                        templateUrl : 'views/home.html',
                        controller  : 'HomeController'
                    },
                    'footer': {
                        templateUrl : 'views/footer.html',
                    }
                }
            })
        
            .state('app.performers', {
                url:'performers',
                views: {
                    'content@': {
                        templateUrl : 'views/performers.html',
                        controller  : 'PerformersController'
                   }
                }
            })
            
            .state('app.recordings', {
                url:'recordings',
                views: {
                    'content@': {
                        templateUrl : 'views/recordings.html',
                        controller  : 'RecordingsController'
                     }
                }
            })

            .state('app.performerEdit', {
                url: 'performers/:id',
                views: {
                    'content@': {
                        templateUrl : 'views/performerEdit.html',
                        controller  : 'PerformerEditController'
                    }
                }
            })

            .state('app.recordingEdit', {
                url: 'recordings/:id',
                views: {
                    'content@': {
                        templateUrl : 'views/recordingEdit.html',
                        controller  : 'RecordingEditController'
                   }
                }
            });
            
            // default route
            $urlRouterProvider.otherwise('/');

/*
            $httpProvider.defaults.useXDomain = true;
            delete $httpProvider.defaults.headers.common['X-Requested-With'];
    
            $sceDelegateProvider.resourceUrlWhitelist(['self',
                                                       'http://localhost:9000/**'
                                                      ]);
*/
    })

;
