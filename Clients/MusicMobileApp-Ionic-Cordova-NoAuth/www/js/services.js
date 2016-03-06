'use strict';

angular.module('MusicApp.services', ['ngResource'])


    .constant("defaultURL", "http://192.168.43.206:9000/")


    .factory('dataService', function($resource, $state, $http) {
    
    
        var factory = {};


        function setErrorMessage(scope, response) {
            if (!response.data && response.status < 200) {
                scope.message = "Error: Connection probably refused. Check URL in Settings!";
            } else {
                scope.message = "Error: " + response.status + ", " + response.statusText;
            }
            // console.log(scope.message);
            console.log(JSON.stringify(response));
        }


        // ===== Service URL + ping ========

        factory.setServiceURL = function(url) {
            factory.serviceURL = url;
        };

        factory.getServiceURL = function() {
            return factory.serviceURL;
        };

        factory.pingService = function(scope, url) {

            scope.message = "";
            
            $resource(url + "ping").get({},
                function(response) {
                    console.log("MusicService pinged, result = " + JSON.stringify(response));
                    scope.message = JSON.stringify(response);
                },
                function(response) {
                    setErrorMessage(scope, response);
                });
        };


        // ===== Performers ========

        factory.performers = function() {
            return $resource(factory.serviceURL + "performers/:id", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };

        factory.performerAddRecordings = function() {
            return $resource(factory.serviceURL + "performers/:id/addRecordings", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };

        factory.performerDeleteRecordings = function() {
            return $resource(factory.serviceURL + "performers/:id/deleteRecordings", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };
        
        factory.queryPerformers = function(scope, criteria) {

            console.log("  --> queryPerformers(): criteria = " + JSON.stringify(criteria));
            scope.message = "";
            scope.performers = [];
            
            factory.performers().query(criteria,
                function(response) {
                    scope.performers = response;
                    console.log("Performers retrieved successfully!");
                    scope.setPerformersChanged(false);
                },
                function(response) {
                    setErrorMessage(scope, response);
                });
        };

        factory.getPerformer = function(scope, id) {

            console.log("  --> getPerformer(): id = " + id);
            scope.message = "";
            scope.performer = {};
            scope.disabledButtonDelete = "disabled";
            
            if (id < 1) {
                return;
            }

            factory.performers().get({id: id})
                .$promise.then(
                    function(response) {
                        scope.performer = response;
                        console.log("Successfully retrieved Performer with id: " + id + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deletePerformer = function(scope, id) {

            console.log("  --> deletePerformer(): id = " + id);
            scope.message = "";

            factory.performers().delete({id: id})
                .$promise.then(
                    function(response) {
                        console.log("Successfully deleted Performer with id: " + id + ", response: " + JSON.stringify(response));
                        scope.setPerformersChanged(true);
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deleteAllPerformers = function(scope) {

            console.log("  --> deleteAllPerformers()");
            scope.message = "";

            factory.performers().delete()
                .$promise.then(
                    function(response) {
                        console.log("Successfully deleted all Performers, response: " + JSON.stringify(response));
                        scope.setPerformersChanged(true);
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.createPerformer = function(scope, performer) {

            console.log("  --> createPerformer(): performer = " + JSON.stringify(performer));
            scope.message = "";
            scope.performer = {};

            factory.performers().save(performer)
                .$promise.then(
                    function(response) {
                        var performer = response;
                        console.log("Successfully created new Performer with id: " + performer.id +
                                    ", response: " + JSON.stringify(response));
                        scope.performers.push(response);     // create returns the new Performer
                        scope.resetPerformerData();
                        scope.setPerformersChanged(true);
                        $state.go('app.performerDetails', {id: performer.id}, {inherit: false});
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.updatePerformer = function(scope, id, performer) {

            console.log("  --> updatePerformer(): id = " + id);
            scope.message = "";
            scope.performer = {};

            factory.performers().update({id: id}, performer)
                .$promise.then(
                    function(response) {
                        scope.performer = response;     // update returns the updated Performer
                        scope.setPerformersChanged(true);
                        console.log("Successfully updated Performer with id: " + id + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.addRecordingsToPerformer = function(scope, pId, rIds) {

            console.log("  --> addRecordingsToPerformer(): pId = " + pId + ", rIds = " + rIds);
            scope.message = "";
            scope.performer = {};

            factory.performerAddRecordings().update({id: pId}, JSON.stringify(rIds))
                .$promise.then(
                    function(response) {
                        scope.performer = response;     // update returns the updated Performer
                        console.log("Successfully added Recordings to Performer with id: " + pId + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deleteRecordingsFromPerformer = function(scope, pId, rIds) {

            console.log("  --> deleteRecordingsFromPerformer(): pId = " + pId + ", rIds = " + rIds);
            scope.message = "";
            scope.performer = {};

            factory.performerDeleteRecordings().update({id: pId}, JSON.stringify(rIds))
                .$promise.then(
                    function(response) {
                        scope.performer = response;     // update returns the updated Performer
                        console.log("Successfully deleted Recordings from Performer with id: " + pId + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

    
        // ===== Recordings ========

        factory.recordings = function() {
            return $resource(factory.serviceURL + "recordings/:id", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };

        factory.recordingAddPerformers = function() {
            return $resource(factory.serviceURL + "recordings/:id/addPerformers", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };

        factory.recordingDeletePerformers = function() {
            return $resource(factory.serviceURL + "recordings/:id/deletePerformers", {},  {
                'update': {
                    method: 'PUT'
                }
            } );
        };

        
        factory.queryRecordings = function(scope, criteria) {

            console.log("  --> queryRecordings(): criteria = " + JSON.stringify(criteria));
            scope.message = "";
            scope.recordings = [];
            
            factory.recordings().query(criteria,
                function(response) {
                    scope.recordings = response;
                    scope.setRecordingsChanged(false);
                    console.log("Recordings retrieved successfully!");
                },
                function(response) {
                    setErrorMessage(scope, response);
                });
        };

        factory.getRecording = function(scope, id) {

            console.log("  --> getRecording(): id = " + id);
            scope.message = "";
            scope.recording = {};
            
            if (id < 1) {
                return;
            }

            factory.recordings().get({id: id})
                .$promise.then(
                    function(response) {
                        scope.recording = response;
                        console.log("Successfully retrieved Recording with id: " + id + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deleteRecording = function(scope, id) {

            console.log("  --> deleteRecording(): id = " + id);
            scope.message = "";

            factory.recordings().delete({id: id})
                .$promise.then(
                    function(response) {
                        console.log("Successfully deleted Recording with id: " + id + ", response: " + JSON.stringify(response));
                        scope.setRecordingsChanged(true);
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deleteAllRecordings = function(scope) {

            console.log("  --> deleteAllRecordings()");
            scope.message = "";

            factory.recordings().delete()
                .$promise.then(
                    function(response) {
                        console.log("Successfully deleted all Recordings, response: " + JSON.stringify(response));
                        scope.setRecordingsChanged(true);
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.createRecording = function(scope, recording, mp3file) {

            console.log("  --> createRecording(): recording = " + JSON.stringify(recording));
            scope.message = "";
            scope.recording = {};
            
            // I looked up the template for this solution at:
            // http://shazwazza.com/post/uploading-files-and-json-data-in-the-same-request-with-angular-js/

            $http({
                    method: 'POST',
                    url: factory.serviceURL + 'recordings',
                
                    // IMPORTANT!!! You might think this should be set to 'multipart/form-data' 
                    // but this is not true because when we are sending up files the request 
                    // needs to include a 'boundary' parameter which identifies the boundary 
                    // name between parts in this multi-part request and setting the Content-type 
                    // manually will not set this boundary parameter. For whatever reason, 
                    // setting the Content-type to 'false' or 'undefined' will force the request to
                    // automatically populate the headers properly including the boundary parameter.
                    headers: { 'Content-Type': undefined },
                
                    // This method will allow us to change how the data is sent up to the server
                    // for which we'll need to encapsulate the model data in 'FormData'
                    transformRequest: function (data) {
                        
                        var formData = new FormData();
                        // need to convert our json object to a string version of json otherwise
                        // the browser will do a 'toString()' on the object which will result 
                        // in the value '[Object object]' on the server.
                        formData.append("meta-data", angular.toJson(data.model));
                        // now add all of the assigned files
                        // for (var i = 0; i < data.files; i++) {
                            //add each file to the form data and iteratively name them
                            // formData.append("file" + i, data.files[i]);
                        // }
                        // In my case it is only one file to upload ...
                        formData.append("data", data.files[0]);
                        return formData;
                    },
                    // Create an object that contains the model and files which will be transformed
                    // in the above transformRequest method
                    data: {
                        model: recording,
                        files: [ mp3file ]
                    }
                })
                .success(function(data /*, status, headers, config */) {
                    var recording = data;
                    console.log("Successfully created new Recording with id: " + recording.id + ", recording: " + recording);
                    scope.recordings.push(recording);     // create returns the new Recording
                    scope.resetRecordingData();
                    scope.setRecordingsChanged(true);
                    $state.go('app.recordingDetails', {id: recording.id}, {inherit: false});
                })
                .error(function(response /*, status, headers, config */) {
                    setErrorMessage(scope, response);
                });
        };

        factory.updateRecording = function(scope, id, recording) {

            console.log("  --> updateRecording(): id = " + id + ", recording = " + JSON.stringify(recording));
            scope.message = "";
            scope.recording = {};

            factory.recordings().update({id: id}, recording)
                .$promise.then(
                    function(response) {
                        scope.recording = response;     // update returns the updated Recording
                        scope.setRecordingsChanged(true);
                        console.log("Successfully updated Recording with id: " + id + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.addPerformersToRecording = function(scope, rId, pIds) {

            console.log("  --> addPerformersToRecording(): rId = " + rId + ", pIds = " + pIds);
            scope.message = "";
            scope.recording = {};

            factory.recordingAddPerformers().update({id: rId}, JSON.stringify(pIds))
                .$promise.then(
                    function(response) {
                        scope.recording = response;     // update returns the updated Recording
                        console.log("Successfully added Performers to Recording with id: " + rId + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };

        factory.deletePerformersFromRecording = function(scope, rId, pIds) {

            console.log("  --> deletePerformersFromRecording(): rId = " + rId + ", pIds = " + pIds);
            scope.message = "";
            scope.recording = {};

            factory.recordingDeletePerformers().update({id: rId}, JSON.stringify(pIds))
                .$promise.then(
                    function(response) {
                        scope.recording = response;     // update returns the updated Recording
                        console.log("Successfully added Performers to Recording with id: " + rId + ", response: " + JSON.stringify(response));
                    },
                    function(response) {
                        setErrorMessage(scope, response);
                    });
        };
    
        return factory;

    })

;
