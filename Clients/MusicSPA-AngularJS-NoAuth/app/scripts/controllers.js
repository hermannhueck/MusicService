'use strict';

angular.module('musicSPA.controllers', [])


    .controller('IndexController', function($scope, dataService, defaultURL) {

        console.log("==> IndexController");


        $scope.defaultURL = defaultURL;

        $scope.setServiceURL = function(url) {
            $scope.serviceURL = url;
            dataService.setServiceURL(url);
        };
        $scope.setServiceURL(defaultURL);

        $scope.pingService = function(url) {
            dataService.pingService($scope, url);
        };


        $scope.hasMessage = function() {
            // console.log("!!!!! " + $scope.message)
            return typeof $scope.message !== 'undefined' && $scope.message !== null && $scope.message.length > 0;
        };

        $scope.isErrorMessage = function() {
            // console.log("!!!!! " + $scope.message)
            return $scope.message.trim().startsWith("Error");
        };

        $scope.clearMessage = function() {
            $scope.message = "";
        };

    
        $scope.performers = [];
        $scope.recordings = [];


        $scope.activeController = "IndexController";
        $scope.homeIsActive = "";
        $scope.performersIsActive = "";
        $scope.recordingsIsActive = "";

        $scope.isActiveController = function(whichController) {
            return $scope.activeController === whichController;
        };
    
        $scope.setActiveController = function(controller) {
            
            $scope.activeController = controller;
            $scope.homeIsActive = "";
            $scope.performersIsActive = "";
            $scope.recordingsIsActive = "";
            $scope.settingsIsActive = "";
            
            if (controller === 'HomeController') {
                $scope.homeIsActive = "active";
            } else if (controller === 'PerformersController') {
                $scope.performersIsActive = "active";
            } else if (controller === 'RecordingsController') {
                $scope.recordingsIsActive = "active";
            } else if (controller === 'SettingsController') {
                $scope.settingsIsActive = "active";
            }
        };


        $scope.queryPerformers = function(criteria) {
            dataService.queryPerformers($scope, criteria);
        };

        $scope.deletePerformer = function(id, requery) {
            dataService.deletePerformer($scope, id, requery);
        };

        $scope.deleteAllPerformers = function(requery) {
            dataService.deleteAllPerformers($scope, requery);
        };


        $scope.queryRecordings = function(criteria) {
            dataService.queryRecordings($scope, criteria);
        };

        $scope.deleteRecording = function(id, requery) {
            dataService.deleteRecording($scope, id, requery);
        };

        $scope.deleteAllRecordings = function(requery) {
            dataService.deleteAllRecordings($scope, requery);
        };


        $scope.currentYear = new Date().getFullYear();

        $scope.optionsPerformerType = [
            { value: "Soloist", label: "Soloist" },
            { value: "Ensemble", label: "Ensemble" },
            { value: "Conductor", label: "Conductor" }
        ];
    
        $scope.messageCannotDeletePerformer =
            "You cannot delete a Performer as long as Recordings are assigned to him. " +
            "First delete the recordings of the performer. Then you can delete the performer.";


        $scope.setPerformersChanged = function(trueOrFalse) {
            $scope.performersChanged = trueOrFalse;
        };
        $scope.setRecordingsChanged = function(trueOrFalse) {
            $scope.recordingsChanged = trueOrFalse;
        };
        
        $scope.setPerformersChanged(false);
        $scope.setRecordingsChanged(false);
                                        
    })


    .controller('HomeController', function($scope) {

        $scope.setActiveController('HomeController');

        console.log("==> HomeController");
    })


    .controller('SettingsController', function($scope) {

        $scope.setActiveController('SettingsController');

        console.log("==> SettingsController");


        $scope.url = { value: $scope.serviceURL };

        $scope.appendSlash = function(url) {
            return url.endsWith('/') ? url : url + '/';
        };

        $scope.changeServiceURL = function() {
            $scope.url.value = $scope.appendSlash($scope.url.value);
            $scope.setServiceURL($scope.url.value);
        };

        $scope.setDefaultServiceURL = function() {
            $scope.url = { value: $scope.defaultURL };
            $scope.setServiceURL($scope.url.value);
        };

        $scope.ping = function() {
            $scope.pingService($scope.appendSlash($scope.url.value));
        };

    })


    .controller('PerformersController', function($scope) {

        $scope.setActiveController('PerformersController');

        console.log("==> PerformersController");

        $scope.showPerformersAsJSON = function() {
            window.open($scope.serviceURL + "performers", "_self");
        };
        $scope.showPerformerAsJSON = function(pId) {
            window.open($scope.serviceURL + "performers/" + pId, "_self");
        };
    
        $scope.displaySearch = false;
        $scope.toggleDisplaySearch = function() {
            console.log("$scope.displaySearch = " + $scope.displaySearch);
            $scope.displaySearch = !$scope.displaySearch;
        };

        $scope.pIdSelected = -1;
        $scope.toggleCollapse = function(pId) {
            $scope.pIdSelected = pId === $scope.pIdSelected ? -1 : pId;
        };
        $scope.isCollapsed = function(pId) {
            return pId !== $scope.pIdSelected;
        };


        $scope.queryRecordings();
        $scope.queryPerformers();


        $scope.resetSearch = function() {
            $scope.criteria = {name: undefined, performerType: undefined, performingIn: undefined};
        };
        $scope.resetSearch();

        $scope.submitSearchCriteria = function() {
            
            console.log($scope.criteria);
            
            $scope.queryPerformers($scope.criteria);
            // $scope.searchForm.$setPristine();
            // $scope.resetSearch();
        };

        $scope.canDeleteAllPerformers = function(performers) {
            
            if (performers.length === 0) {
                return false;
            }
            
            var count = performers.length;
            for (var i = 0; i < count; i++) {
                if (performers[i].recordings.length > 0) {
                    return false;
                }
            }
            return true;
        };


        // watch variable performersChanged (defined in AppController)
        $scope.$watch(function(scope) {
            return scope.performersChanged;
        }, function(performersChangedNewValue, performersChangedOldValue) {
            console.log("performersChanged: " + performersChangedOldValue + " --> " + performersChangedNewValue);
            if (performersChangedNewValue === true) {
                $scope.queryPerformers();
            }
        });

    })


    .controller('RecordingsController', function($scope, $sce) {

        $scope.setActiveController('RecordingsController');

        $scope.showRecordingsAsJSON = function() {
            window.open($scope.serviceURL + "recordings", "_self");
        };
        $scope.showRecordingAsJSON = function(rId) {
            window.open($scope.serviceURL + "recordings/" + rId, "_self");
        };

        console.log("==> RecordingsController");

        $scope.displaySearch = false;
        $scope.toggleDisplaySearch = function() {
            $scope.displaySearch = !$scope.displaySearch;
        };


        $scope.rIdSelected = -1;
        $scope.toggleCollapse = function(rId) {
            $scope.rIdSelected = rId === $scope.rIdSelected ? -1 : rId;
        };
        $scope.isCollapsed = function(rId) {
            return rId !== $scope.rIdSelected;
        };


        $scope.getAudioUrl = function(rId) {
            var audioUrl = $scope.serviceURL + "recordings/" + rId + "/data";
            return $sce.trustAsResourceUrl(audioUrl);
        };


        $scope.queryRecordings();
        $scope.queryPerformers();

                                        
        $scope.resetSearch = function() {
            $scope.criteria = {title: undefined, composer: undefined, yearMin: undefined, yearMax: undefined, performedBy: undefined};
        };
        $scope.resetSearch();

        $scope.isValidYear = function(year) {
            if (typeof year === 'undefined' || year === null) {
                return true;
            }
            if (isNaN(year)) {
                return false;
            }
            var y = parseInt(year.toString, 10);
            return y >= 1900 && y <= $scope.currentYear;
        };
                                        
        $scope.submitSearchCriteria = function() {
            
            console.log($scope.criteria);
            
            if (!$scope.isValidYear($scope.criteria.yearMin) || !$scope.isValidYear($scope.criteria.yearMax)) {
                $scope.invalidSearchInput = true;
                $scope.searchInputMessage = "Error: Min. Year or Max. Year is not between 1900 and " + $scope.currentYear + ".";
            } else {
                $scope.invalidSearchInput = false;
                $scope.queryRecordings($scope.criteria);
                // $scope.searchForm.$setPristine();
                // $scope.resetSearch();
            }
        };


        // watch variable performersChanged (defined in AppController)
        $scope.$watch(function(scope) {
            return scope.recordingsChanged;
        }, function(recordingsChangedNewValue, recordingsChangedOldValue) {
            console.log("recordingsChanged: " + recordingsChangedOldValue + " --> " + recordingsChangedNewValue);
            if (recordingsChangedNewValue === true) {
                $scope.queryRecordings();
            }
        });

    })


    .controller('PerformerEditController', function($scope, $stateParams, dataService) {

        $scope.setActiveController('PerformerEditController');

        var pId = (typeof $stateParams.id === 'undefined') ? -1 : parseInt($stateParams.id, 10);

        console.log("==> PerformerEditController (pId = " + pId + ")");
    
        $scope.pId = pId;
        $scope.isNew = pId < 1;
        $scope.isEdit = !$scope.isNew;
    
        $scope.buttonUpdateLabel = $scope.isEdit ? "Update" : "Create";
        $scope.buttonUpdateTooltip = $scope.isEdit ? "Update this Performer" : "Create new Performer";
        $scope.buttonCancelTooltip = $scope.isEdit ? "Cancel Editing this Performer" : "Cancel creating new Performer";
        $scope.recordingsToDelete = { ids: [] };
        $scope.recordingsToAdd = { ids: [] };


        $scope.getPerformer = function(pId) {
            dataService.getPerformer($scope, pId);
        };

        $scope.deleteRecordingsFromPerformer = function() {
            
            console.log("recordingsToDelete.ids = " + $scope.recordingsToDelete.ids);
            dataService.deleteRecordingsFromPerformer($scope, pId, $scope.recordingsToDelete.ids);
        };

        $scope.addRecordingsToPerformer = function() {
            
            console.log("recordingsToAdd.ids = " + $scope.recordingsToAdd.ids);
            dataService.addRecordingsToPerformer($scope, pId, $scope.recordingsToAdd.ids);
        };

        $scope.submitPerformer = function() {
            
            console.log("submitPerformer() " + JSON.stringify($scope.performer));
            
            if ($scope.isNew) {
                
                $scope.performer.recordings = [];
                var p = {
                    performer: $scope.performer,
                    recordingIds: []
                };
                
                dataService.createPerformer($scope, p);
            } else {
                dataService.updatePerformer($scope, pId, $scope.performer);
            }
        };


        if ($scope.isEdit) {
            $scope.getPerformer(pId);
        } else {
            $scope.performer = {};
        }
        $scope.queryRecordings();
    
    })


    .filter('filterRecordingsNotAssignedToCurrentPerformer', function () {

        return function (recordings, performer) {
            return recordings.filter(function(recording) {
                if (typeof performer === 'undefined' || typeof performer.recordings === 'undefined' || performer.recordings.length === 0) {
                    return true;
                }
                return performer.recordings.map( function(r) {return r.id;} ).indexOf(recording.id) < 0;
            });
        };
    })


    .controller('RecordingEditController', function($scope, $stateParams, dataService) {

        $scope.setActiveController('RecordingEditController');

        var rId = (typeof $stateParams.id === 'undefined') ? -1 : parseInt($stateParams.id, 10);

        console.log("==> RecordingEditController (rId = " + rId + ")");
    
        $scope.rId = rId;
        $scope.isNew = rId < 1;
        $scope.isEdit = !$scope.isNew;
    
        $scope.buttonUpdateLabel = $scope.isEdit ? "Update" : "Create";
        $scope.buttonUpdateTooltip = $scope.isEdit ? "Update this Recording" : "Create new Recording";
        $scope.buttonCancelTooltip = $scope.isEdit ? "Cancel Editing this Recording" : "Cancel creating new Recording";
        $scope.performersToDelete = { ids: [] };
        $scope.performersToAdd = { ids: [] };
        $scope.recording = {};
        $scope.mp3file = {};


        $scope.getRecording = function(rId) {
            dataService.getRecording($scope, rId);
        };

        $scope.deletePerformersFromRecording = function() {
            
            console.log("performersToDelete.ids = " + $scope.performersToDelete.ids);
            dataService.deletePerformersFromRecording($scope, rId, $scope.performersToDelete.ids);
        };

        $scope.addPerformersToRecording = function() {
            
            console.log("performersToAdd.ids = " + $scope.performersToAdd.ids);
            dataService.addPerformersToRecording($scope, rId, $scope.performersToAdd.ids);
        };

        $scope.fileChanged = function(element) {
            
            function trim(str) {
                if (!str) {
                    return "";
                } else {
                    return str.replace(/\W/g, " ").trim();
                }
            }
            
            console.log("fileChanged(): element = " + element + " / " + JSON.stringify(element));
            var mp3file = element.files[0];
            console.log("mp3file = " + mp3file + " / " + JSON.stringify(mp3file));
            console.log("mp3file.name = " + JSON.stringify(mp3file.name));

            id3(mp3file, function(err, tags) {

                console.log(err, tags);
                console.log("tags.title = " + tags.title);
                console.log("tags.artist = " + tags.artist);
                console.log("tags.year = " + tags.year);
                
                $scope.$apply(function() {
                    $scope.mp3file = mp3file;
                    var t = trim(tags.title);
                    $scope.recording.title = t ? t : mp3file.name;
                    var a = trim(tags.artist);
                    $scope.recording.composer = a ? a : mp3file.name;
                    var y = trim(tags.year); 
                    $scope.recording.year = y ? y : 1900;
                });
            });
        };

        $scope.submitRecording = function() {
            
            console.log("submitRecording(): " + JSON.stringify($scope.recording));
            
            if ($scope.isEdit) {
                dataService.updateRecording($scope, rId, $scope.recording);
            } else {
                // isNew
                console.log("submitRecording(): mp3file.name = " + $scope.mp3file.name);
                
                $scope.recording.id = rId;
                $scope.recording.performers = [];

                var r = {
                    recording: $scope.recording,
                    performerIds: []
                };
                
                dataService.createRecording($scope, r, $scope.mp3file);
            }
        };


        if ($scope.isEdit) {
            $scope.getRecording(rId);
        }
        $scope.queryPerformers();
    
    })


    .filter('filterPerformersNotAssignedToCurrentRecording', function () {

        return function (performers, recording) {
            return performers.filter(function(performer) {
                if (typeof recording === 'undefined' || typeof recording.performers === 'undefined' || recording.performers.length === 0) {
                    return true;
                }
                return recording.performers.map( function(p) {return p.id;} ).indexOf(performer.id) < 0;
            });
        };
    })

;
