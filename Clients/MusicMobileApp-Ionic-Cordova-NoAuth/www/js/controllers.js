'use strict';

angular.module('MusicApp.controllers', [])


    .controller('AppController', function($scope, dataService, defaultURL, $ionicPopup, $state) {

        console.log("==> AppController");


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

    
        $scope.performers = [];
        $scope.recordings = [];


        $scope.queryPerformers = function(criteria) {
            dataService.queryPerformers($scope, criteria);
        };

        $scope.deletePerformer = function(pId) {
            dataService.deletePerformer($scope, pId);
        };

        $scope.deleteAllPerformers = function() {
            dataService.deleteAllPerformers($scope);
        };


        $scope.queryRecordings = function(criteria) {
            dataService.queryRecordings($scope, criteria);
        };

        $scope.deleteRecording = function(id) {
            dataService.deleteRecording($scope, id);
        };

        $scope.deleteAllRecordings = function() {
            dataService.deleteAllRecordings($scope);
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


        $scope.canDeletePerformer = function(performer) {
            var retVal = false;
            if (!performer) {
                //console.log("canDeletePerformer(): performer undefined");
                retVal = false;
            } else if (!performer.recordings) {
                //console.log("canDeletePerformer(): performer.recordings undefined");
                retVal = true;
            } else if (performer.recordings.length === 0) {
                //console.log("canDeletePerformer(): performer has no recordings");
                retVal = true;
            } else {
                //console.log("canDeletePerformer(): performer HAS recordings");
                retVal = false;
            }
            //console.log("canDeletePerformer(): performer to delete: " + JSON.stringify(performer) + ", returns: " + retVal);
            return retVal;
        };

        $scope.canDeleteAllPerformers = function(performers) {
            
            if (!performers || performers.length === 0) {
                return false;
            }
            
            var count = performers.length;
            for (var i = 0; i < count; i++) {
                if (!$scope.canDeletePerformer(performers[i])) {
                    return false;
                }
            }
            return true;
        };


        $scope.deletePerformer2 = function(p) {
            //console.log(">>>>>>>> deletePerformer2(): canDeletePerformer(p): " + $scope.canDeletePerformer(p))
            if ($scope.canDeletePerformer(p)) {
                $scope.confirmDeletePerformer(p);
            } else {
                console.log("cannot delete performer: " + p);
                $scope.alertCannotDelete(p);
            }
        };

        $scope.alertCannotDelete = function(p) {

            $ionicPopup.alert({
                title: 'Cannot delete Performer (id = ' + p.id + ')',
                template: $scope.messageCannotDeletePerformer,
                okType: 'button-positive button-outline',
            }).then(function(ok) {
                // noop
            });
        };

        $scope.confirmDeletePerformer = function(p) {

            $ionicPopup.confirm({
                title: 'Delete Performer (id = ' + p.id + ')',
                template: 'You really want to delete Performer with id: ' + p.id + ' and name: "' + p.name + '"?',
                okType: 'button-positive button-outline',
                cancelType: 'button-dark button-outline',
            }).then(function(confirmed) {
                if (confirmed) {
                    $scope.deletePerformer(p.id);
                    $state.go('app.performers');
                }
            });
        };


        $scope.deleteRecording2 = function(r) {
            //console.log(">>>>>>>> deleteRecording2(): r: ")
            $scope.confirmDeleteRecording(r);
        };

        $scope.confirmDeleteRecording = function(r) {

            $ionicPopup.confirm({
                title: 'Delete Recording (id = ' + r.id + ')',
                template: 'You really want to delete Recording with id: ' + r.id + ' and title: "' + r.title + '"?',
                okType: 'button-positive button-outline',
                cancelType: 'button-dark button-outline',
            }).then(function(confirmed) {
                if (confirmed) {
                    $scope.deleteRecording(r.id);
                    $state.go('app.recordings');
                }
            });
        };


        $scope.setPerformersChanged = function(trueOrFalse) {
            $scope.performersChanged = trueOrFalse;
        }
        $scope.setRecordingsChanged = function(trueOrFalse) {
            $scope.recordingsChanged = trueOrFalse;
        }
        
        $scope.setPerformersChanged(false);
        $scope.setRecordingsChanged(false);


        $scope.performerById = function(pId) {
            return $scope.performers.filter(function(p) {
                return p.id === pId;
            }).shift();
        };
    
        $scope.recordingById = function(rId) {
            return $scope.recordings.filter(function(r) {
                return r.id === rId;
            }).shift();
        };

        $scope.goToPerformers = function() {
            $state.go('app.performers');
        };
    
        $scope.goToRecordings = function() {
            $state.go('app.recordings');
        };
                                        
    })


    .controller('HomeController', function($scope) {

        console.log("==> HomeController");
    })


    .controller('ContactController', function($scope) {

        console.log("==> ContactController");
    })


    .controller('SettingsController', function($scope) {

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


    .controller('PerformersController', function($scope, dataService, $ionicPopover, $ionicModal,
                                                  $ionicPlatform, $cordovaLocalNotification, $cordovaToast) {

        console.log("==> PerformersController");


        $scope.queryRecordings();
        $scope.queryPerformers();
                        

        $scope.textFilters = ["", "Soloist", "Ensemble", "Conductor"];    
        $scope.isSelected = function(tabToCheck) {
            return $scope.tabIndex === tabToCheck;
        };
        $scope.select = function(newTabIndex) {
            $scope.tabIndex = newTabIndex > 3 || newTabIndex < 0 ? 0 : newTabIndex;
        };
        $scope.select(0);


        // ===== Performers Popover ===============

        $ionicPopover.fromTemplateUrl('templates/performers-popover.html', {
            scope: $scope
        }).then(function(popover) {
            $scope.popover = popover;
        });

        $scope.$on('$destroy', function() {
            $scope.popover.remove();
        });


        // ===== Search Modal ===============

        $ionicModal.fromTemplateUrl('templates/performers-search-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.searchModal = modal;
        });
                                        
        $scope.resetCriteria = function() {
            $scope.criteria = {name: undefined, performerType: undefined, performingIn: undefined};
        };
        $scope.resetCriteria();

        $scope.searchPerformers = function() {

            console.log('Searching for Performers using criteria: ', JSON.stringify($scope.criteria));

            $scope.queryPerformers($scope.criteria);
            $scope.searchModal.hide();
        };
    
        $scope.hasSearchCriteria = function() {
            var c = $scope.criteria;
            return c.name || c.performerType || c.performingIn;
        };


        // ===== New Performer Modal ===============

        $ionicModal.fromTemplateUrl('templates/performer-new-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalNewPerformer = modal;
        });

        $scope.$on('modal.shown', function() {
            if ($scope.modalNewPerformer.isShown()) {
                $scope.resetPerformerData();
            }
        });

        $scope.resetPerformerData = function() {
            $scope.performerData = {name: undefined, performerType: undefined, recordings: undefined};
        }
        $scope.resetPerformerData();

        $scope.newPerformer = function() {

            console.log('newPerformer(): Creating new Performer using performerData: ', $scope.performerData);

            $scope.performerData.recordings = [];
            var p = {
                performer: $scope.performerData,
                recordingIds: []
            };

            dataService.createPerformer($scope, p);
            $scope.modalNewPerformer.hide();
                
            $ionicPlatform.ready(function () {

                // add notification to notification bar
                $cordovaLocalNotification
                    .schedule({
                        id: 1,
                        title: "Added Performer",
                        text: $scope.performer.name
                    }).then(function() {
                        console.log('Successfully created notification about adding performer ' + $scope.performerData.name);
                    }, function() {
                        console.log('Failed to create notification');
                    });

                // show toast message in the bottom of the screen
                $cordovaToast
                  .show('Added Performer ' + $scope.performerData.name, 'long', 'bottom')
                  .then(function (success) {
                      // success
                  }, function (error) {
                      // error
                  });
            });
        };


        // watch variable performersChanged (defined in AppController)
        $scope.$watch(function(scope) {
            return scope.performersChanged;
        }, function(performersChangedNewValue, performersChangedOldValue) {
            console.log("performersChanged: " + performersChangedOldValue + " --> " + performersChangedNewValue)
            if (performersChangedNewValue === true) {
                $scope.queryPerformers();
            }
        });


        $scope.shouldShowDelete = false;
        $scope.toggleDelete = function () {
            $scope.shouldShowDelete = !$scope.shouldShowDelete;
        }
        $scope.toggleDeleteLabel = function () {
            return $scope.shouldShowDelete ? "Hide Delete Buttons" : "Show Delete Buttons";
        }


        $scope.shouldShowReorder = false;
        $scope.toggleReorder = function () {
            $scope.shouldShowReorder = !$scope.shouldShowReorder;
        }
        $scope.toggleReorderLabel = function () {
            return $scope.shouldShowReorder ? "Hide Reorder Buttons" : "Show Reorder Buttons";
        }
        $scope.moveListItem = function(item, fromIndex, toIndex) {
            // Move the item in the array
            $scope.performers.splice(fromIndex, 1);
            $scope.performers.splice(toIndex, 0, item);
        };

    })


    .controller('RecordingsController', function($scope, $sce, dataService, $ionicPopover, $ionicModal,
                                                  $ionicPlatform, $cordovaLocalNotification, $cordovaToast, $document) {

        console.log("==> RecordingsController");


        $scope.queryRecordings();
        $scope.queryPerformers();


        // ===== Recordings Popover ===============

        $ionicPopover.fromTemplateUrl('templates/recordings-popover.html', {
            scope: $scope
        }).then(function(popover) {
            $scope.popover = popover;
        });

        $scope.$on('$destroy', function() {
            $scope.popover.remove();
        });


        // ===== Search Modal ===============

        $ionicModal.fromTemplateUrl('templates/recordings-search-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.searchModal = modal;
        });
                                        
        $scope.resetCriteria = function() {
            $scope.criteria = {title: undefined, composer: undefined, yearMin: undefined, yearMax: undefined, performedBy: undefined};
        };
        $scope.resetCriteria();

        $scope.searchRecordings = function() {

            console.log('Searching for Recordings using criteria: ', JSON.stringify($scope.criteria));

            $scope.queryRecordings($scope.criteria);
            $scope.searchModal.hide();
        };
    
        $scope.hasSearchCriteria = function() {
            var c = $scope.criteria;
            return c.title || c.composer || c.yearMin || c.yearMax || c.performedBy;
        };


        // ===== New Recording Modal ===============

        $ionicModal.fromTemplateUrl('templates/recording-new-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalNewRecording = modal;
        });

        $scope.$on('modal.shown', function() {
            if ($scope.modalNewRecording.isShown()) {
                $scope.resetRecordingData();
            }
        });

        $scope.resetRecordingData = function() {
            $scope.recordingData = {title: undefined, composer: undefined, year: undefined, performers: undefined};
            $scope.mp3file = {};
            angular.element($document[0].getElementById('mp3file')).val(null);
        }
        
        $scope.resetRecordingData();

        $scope.newRecording = function() {

            console.log('newRecording(): Creating new Recording using recordingData: ', $scope.recordingData);
            console.log("newRecording(): mp3file.name = " + $scope.mp3file.name);

            $scope.recordingData.performers = [];

            var r = {
                recording: $scope.recordingData,
                performerIds: []
            };

            dataService.createRecording($scope, r, $scope.mp3file);
            $scope.modalNewRecording.hide();
                
            $ionicPlatform.ready(function () {

                // add notification to notification bar
                $cordovaLocalNotification
                    .schedule({
                        id: 1,
                        title: "Added Recording",
                        text: $scope.recordingData.name
                    }).then(function() {
                        console.log('Successfully created notification about adding recording ' + $scope.recordingData.name);
                    }, function() {
                        console.log('Failed to create notification');
                    });

                // show toast message in the bottom of the screen
                $cordovaToast
                  .show('Added Recording ' + $scope.recording.name, 'long', 'bottom')
                  .then(function (success) {
                      // success
                  }, function (error) {
                      // error
                  });
            });
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
                    $scope.recordingData.title = t ? t : mp3file.name;
                    var a = trim(tags.artist);
                    $scope.recordingData.composer = a ? a : mp3file.name;
                    var y = parseInt(trim(tags.year), 10); 
                    $scope.recordingData.year = y ? y : $scope.currentYear;
                });
            });
        };


        // watch variable performersChanged (defined in AppController)
        $scope.$watch(function(scope) {
            return scope.recordingsChanged;
        }, function(recordingsChangedNewValue, recordingsChangedOldValue) {
            console.log("recordingsChanged: " + recordingsChangedOldValue + " --> " + recordingsChangedNewValue)
            if (recordingsChangedNewValue === true) {
                $scope.queryRecordings();
            }
        });


        $scope.shouldShowDelete = false;
        $scope.toggleDelete = function () {
            $scope.shouldShowDelete = !$scope.shouldShowDelete;
        }
        $scope.toggleDeleteLabel = function () {
            return $scope.shouldShowDelete ? "Hide Delete Buttons" : "Show Delete Buttons";
        }


        $scope.shouldShowReorder = false;
        $scope.toggleReorder = function () {
            $scope.shouldShowReorder = !$scope.shouldShowReorder;
        }
        $scope.toggleReorderLabel = function () {
            return $scope.shouldShowReorder ? "Hide Reorder Buttons" : "Show Reorder Buttons";
        }
        $scope.moveListItem = function(item, fromIndex, toIndex) {
            // Move the item in the array
            $scope.recordings.splice(fromIndex, 1);
            $scope.recordings.splice(toIndex, 0, item);
        };
    })


    .controller('PerformerDetailsController', function($scope, $stateParams, dataService, $ionicModal) {

        var pId = (typeof $stateParams.id === 'undefined') ? -1 : parseInt($stateParams.id, 10);

        console.log("==> PerformerDetailsController (pId = " + pId + ")");

        dataService.getPerformer($scope, pId);


        // ===== Edit Performer Modal ===============

        $ionicModal.fromTemplateUrl('templates/performer-edit-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalEditPerformer = modal;
        });
    
        $scope.resetPerformerData = function() {
            dataService.getPerformer($scope, pId);
        }
        $scope.resetPerformerData();

        $scope.updatePerformer = function() {

            console.log('updatePerformer(): Updating Performer using performerData: ', JSON.stringify($scope.performer));

            dataService.updatePerformer($scope, pId, $scope.performer);
            $scope.modalEditPerformer.hide();
        };


        // ===== Assign-Recordings-to-Performer Modal ===============

        $ionicModal.fromTemplateUrl('templates/performer-assignRecordings-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalAssignRecordingsToPerformer = modal;
        });
    
        $scope.recordingsToDelete = { ids: [] };
        $scope.recordingsToAdd = { ids: [] };

        $scope.deleteRecordingsFromPerformer = function() {
            
            console.log("recordingsToDelete.ids = " + $scope.recordingsToDelete.ids);
            dataService.deleteRecordingsFromPerformer($scope, pId, $scope.recordingsToDelete.ids);
        };

        $scope.addRecordingsToPerformer = function() {
            
            console.log("recordingsToAdd.ids = " + $scope.recordingsToAdd.ids);
            dataService.addRecordingsToPerformer($scope, pId, $scope.recordingsToAdd.ids);
        };
    
    })


    .controller('RecordingDetailsController', function($scope, $stateParams, dataService, $sce, $ionicModal) {

        var rId = (typeof $stateParams.id === 'undefined') ? -1 : parseInt($stateParams.id, 10);

        console.log("==> RecordingDetailsController (rId = " + rId + ")");

        dataService.getRecording($scope, rId);

        $scope.getAudioUrl = function(rId) {
            var audioUrl = $scope.serviceURL + "recordings/" + rId + "/data";
            return $sce.trustAsResourceUrl(audioUrl);
        };


        // ===== Edit Recording Modal ===============

        $ionicModal.fromTemplateUrl('templates/recording-edit-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalEditRecording = modal;
        });
    
        $scope.resetRecordingData = function() {
            dataService.getRecording($scope, rId);
        }
        $scope.resetRecordingData();

        $scope.updateRecording = function() {

            console.log('updateRecording(): Updating Recording using recordingData: ', JSON.stringify($scope.recording));

            dataService.updateRecording($scope, rId, $scope.recording);
            $scope.modalEditRecording.hide();
        };


        // ===== Assign-Performers-to-Recording Modal ===============

        $ionicModal.fromTemplateUrl('templates/recording-assignPerformers-modal.html', {
            scope: $scope,
            animation: 'slide-in-up'
        }).then(function(modal) {
            $scope.modalAssignPerformersToRecording = modal;
        });
    
        $scope.performersToDelete = { ids: [] };
        $scope.performersToAdd = { ids: [] };

        $scope.deletePerformersFromRecording = function() {
            
            console.log("performersToDelete.ids = " + $scope.performersToDelete.ids);
            dataService.deletePerformersFromRecording($scope, rId, $scope.performersToDelete.ids);
        };

        $scope.addPerformersToRecording = function() {
            
            console.log("performersToAdd.ids = " + $scope.performersToAdd.ids);
            dataService.addPerformersToRecording($scope, rId, $scope.performersToAdd.ids);
        };

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
