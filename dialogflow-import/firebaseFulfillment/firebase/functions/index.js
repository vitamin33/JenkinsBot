'use strict';

const functions = require('firebase-functions'); // Cloud Functions for Firebase library
const DialogflowApp = require('actions-on-google').DialogflowApp; // Google Assistant helper library
const request = require('request');

var host = '';
var username = '';
var password = '';

exports.dialogflowFirebaseFulfillment = functions.https.onRequest((request, response) => {
  console.log('Dialogflow Request headers: ' + JSON.stringify(request.headers));
  console.log('Dialogflow Request body: ' + JSON.stringify(request.body));
  if (request.body.result) {
    processV1Request(request, response);
  } else {
    console.log('Invalid Request');
    return response.status(400).end('Invalid Webhook Request (expecting v1 webhook request)');
  }
});
/*
* Function to handle v1 webhook requests from Dialogflow
*/
function processV1Request (request, response) {
  let action = request.body.result.action; // https://dialogflow.com/docs/actions-and-parameters
  let parameters = request.body.result.parameters; // https://dialogflow.com/docs/actions-and-parameters
  let inputContexts = request.body.result.contexts; // https://dialogflow.com/docs/contexts
  let requestSource = (request.body.originalRequest) ? request.body.originalRequest.source : undefined;
  const googleAssistantRequest = 'google'; // Constant to identify Google Assistant requests
  const app = new DialogflowApp({request: request, response: response});
  
  
  host = inputContexts[0].parameters.host;
  username = inputContexts[0].parameters.user; 
  password = inputContexts[0].parameters.password; 

 
  console.log('host:', host);
  console.log('username:', username);
    
  let job = parameters['job']; 
  
  // Create handlers for Dialogflow actions as well as a 'default' handler
  const actionHandlers = {
    // The default welcome intent has been matched, welcome the user (https://dialogflow.com/docs/events#default_welcome_intent)
    'input.welcome': () => {
      // Use the Actions on Google lib to respond to Google requests; for other requests use JSON
      if (requestSource === googleAssistantRequest) {
        sendGoogleResponse('Hello, Welcome to my Jenkins agent!'); // Send simple response to user
      } else {
        sendResponse('Hello, Welcome to my Jenkins agent!'); // Send simple response to user
      }
    },
    'input.build': () => {

      callBuildJob(job).then((output) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': output, 'displayText': output }));
      }).catch((error) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': error, 'displayText': error }));
      });
    },
    'input.restart': () => {

      callRestart().then((output) => {
          
        console.log('callRestart(), output :', output);
        response.send(JSON.stringify({ 'speech': output, 'displayText': output }));
      }).catch((error) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': error, 'displayText': error }));
      });
    },
    'input.show_jobs': () => {

      callShowJobs(response).then((output) => {
          
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': output, 'displayText': output }));
      }).catch((error) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': error, 'displayText': error }));
      });
    },
    'input.show_build_status': () => {

      callShowBuildStatus(job, response).then((output) => {
          
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': output, 'displayText': output }));
      }).catch((error) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': error, 'displayText': error }));
      });
    },
    'input.download_build': () => {

      callDownloadBuild(job, response).then((output) => {
          
        response.setHeader('Content-Type', 'application/json');
      }).catch((error) => {
        response.setHeader('Content-Type', 'application/json');
        response.send(JSON.stringify({ 'speech': error, 'displayText': error }));
      });
    },
    // The default fallback intent has been matched, try to recover (https://dialogflow.com/docs/intents#fallback_intents)
    'input.unknown': () => {
      // Use the Actions on Google lib to respond to Google requests; for other requests use JSON
      if (requestSource === googleAssistantRequest) {
        sendGoogleResponse('I\'m having trouble, can you try that again?'); // Send simple response to user
      } else {
        sendResponse('I\'m having trouble, can you try that again?'); // Send simple response to user
      }
    },
    // Default handler for unknown or undefined actions
    'default': () => {
      // Use the Actions on Google lib to respond to Google requests; for other requests use JSON
      if (requestSource === googleAssistantRequest) {
        let responseToUser = {
          googleRichResponse: googleRichResponse, // Optional, uncomment to enable
          speech: 'This message is from JenkinsBot\'s Cloud Functions', // spoken response
          text: 'This message is from JenkinsBot\'s Cloud Functions :-)' // displayed response
        };
        sendGoogleResponse(responseToUser);
      } else {
        let responseToUser = {
          //data: richResponsesV1, // Optional, uncomment to enable
          speech: 'This message is from JenkinsBot\'s Cloud Functions', // spoken response
          text: 'This message is from JenkinsBot\'s Cloud Functions :-)' // displayed response
        };
        sendResponse(responseToUser);
      }
    }
  };
  // If undefined or unknown action use the default handler
  if (!actionHandlers[action]) {
    action = 'default';
  }
  // Run the proper handler function to handle the request from Dialogflow
  actionHandlers[action]();
    // Function to send correctly formatted Google Assistant responses to Dialogflow which are then sent to the user
  function sendGoogleResponse (responseToUser) {
    if (typeof responseToUser === 'string') {
      app.ask(responseToUser); // Google Assistant response
    } else {
      // If speech or displayText is defined use it to respond
      let googleResponse = app.buildRichResponse().addSimpleResponse({
        speech: responseToUser.speech || responseToUser.displayText,
        displayText: responseToUser.displayText || responseToUser.speech
      });
      // Optional: Overwrite previous response with rich response
      if (responseToUser.googleRichResponse) {
        googleResponse = responseToUser.googleRichResponse;
      }
      // Optional: add contexts (https://dialogflow.com/docs/contexts)
      if (responseToUser.googleOutputContexts) {
        app.setContext(...responseToUser.googleOutputContexts);
      }
      console.log('Response to Dialogflow (AoG): ' + JSON.stringify(googleResponse));
      app.ask(googleResponse); // Send response to Dialogflow and Google Assistant
    }
  }
  // Function to send correctly formatted responses to Dialogflow which are then sent to the user
  function sendResponse (responseToUser) {
    // if the response is a string send it as a response to the user
    if (typeof responseToUser === 'string') {
      let responseJson = {};
      responseJson.speech = responseToUser; // spoken response
      responseJson.displayText = responseToUser; // displayed response
      response.json(responseJson); // Send response to Dialogflow
    } else {
      // If the response to the user includes rich responses or contexts send them to Dialogflow
      let responseJson = {};
      // If speech or displayText is defined, use it to respond (if one isn't defined use the other's value)
      responseJson.speech = responseToUser.speech || responseToUser.displayText;
      responseJson.displayText = responseToUser.displayText || responseToUser.speech;
      // Optional: add rich messages for integrations (https://dialogflow.com/docs/rich-messages)
      responseJson.data = responseToUser.data;
      // Optional: add contexts (https://dialogflow.com/docs/contexts)
      responseJson.contextOut = responseToUser.outputContexts;
      console.log('Response to Dialogflow: ' + JSON.stringify(responseJson));
      response.json(responseJson); // Send response to Dialogflow
    }
  }
}
// Construct rich response for Google Assistant (v1 requests only)
const app = new DialogflowApp();
const googleRichResponse = app.buildRichResponse()
  .addSimpleResponse('This is the first simple response for Google Assistant')
  .addSuggestions(
    ['Suggestion Chip', 'Another Suggestion Chip'])
    // Create a basic card and add it to the rich response
  .addBasicCard(app.buildBasicCard(`This is a basic card.  Text in a
 basic card can include "quotes" and most other unicode characters
 including emoji 📱.  Basic cards also support some markdown
 formatting like *emphasis* or _italics_, **strong** or __bold__,
 and ***bold itallic*** or ___strong emphasis___ as well as other things
 like line  \nbreaks`) // Note the two spaces before '\n' required for a
                        // line break to be rendered in the card
    .setSubtitle('This is a subtitle')
    .setTitle('Title: this is a title')
    .addButton('This is a button', 'https://assistant.google.com/')
    .setImage('https://developers.google.com/actions/images/badges/XPM_BADGING_GoogleAssistant_VER.png',
      'Image alternate text'))
  .addSimpleResponse({ speech: 'This is another simple response',
    displayText: 'This is the another simple response 💁' });

function callBuildJob (job) {
  return new Promise((resolve, reject) => {

    let path = '/job/' + encodeURIComponent(job) + '/build';
    var url = 'http://' + username + ':' + password + '@' + host + path;
    
    console.log('API Request: ' + url);
    request.post({url: url}, function (error, response, body) {
        console.log('error:', error);
        console.log('statusCode:', response && response.statusCode);
        console.log('body:', body);
        
        if (response.statusCode == "201") {
            let output = 'Build was started for job ' + job;
            resolve(output)
        } else {
            let output = 'Error while starting build for job ' + job;
            resolve(output);
        }
    });
  });
}

function callShowJobs (response) {
  return new Promise((resolve, reject) => {

    let path = '/api/json?tree=jobs[name,color]';

    var url = 'http://' + username + ':' + password + '@' + host + path;
    
    console.log('API Request: ' + url);
    
    request({url: url}, function (error, res, body) {
        console.log('error:', error);
        console.log('response:', res);
        console.log('body:', body);
        
        if (res.statusCode == "200") {
            var jsonData = JSON.parse(body);
            var jobsResponse = {
                'jobs': []
            };
            
            for (var i in jsonData.jobs) {
                jobsResponse.jobs.push({
                    "name": jsonData.jobs[i].name, 
                    "status":jsonData.jobs[i].color
                });
            }
            let output = 'Jenkins has next jobs: ';
            response.send(JSON.stringify({ 'data':jobsResponse, 'speech': output, 'displayText': output }));
            
            resolve(jobsResponse)
        } else {
            let output = 'Error while getting jobs list!';
            resolve(output);
        }
    });

  });
}

function callShowBuildStatus(job, response) {
  return new Promise((resolve, reject) => {

    let path = '/job/' + encodeURIComponent(job) + '/lastBuild/api/json';
    var url = 'http://' + username + ':' + password + '@' + host + path;
    
    console.log('API Request: ' + url);

    request({url: url}, function (error, res, body) {
        console.log('error:', error);
        console.log('response:', res);
        console.log('body:', body);
        
        if (res.statusCode == "200") {
            var jsonData = JSON.parse(body);

            let output = 'Build name: ' + jsonData.fullDisplayName + ', status:' + jsonData.result + ', duration:' + jsonData.duration;
            
            resolve(output)
        } else {
            let output = 'No last successful build for this project!';
            resolve(output);
        }
    });

  });
}

function callDownloadBuild(job, response) {
  return new Promise((resolve, reject) => {

    let path = '/job/' + encodeURIComponent(job) + '/lastSuccessfulBuild/api/json';
    var url = 'http://' + username + ':' + password + '@' + host + path;
    
    console.log('API Request: ' + url);

    request({url: url}, function (error, res, body) {
        console.log('error:', error);
        console.log('response:', res);
        console.log('body:', body); 
        
        if (res.statusCode == "200") {
            var jsonData = JSON.parse(body);
            var jsonResponse = {};
            
            var output = '';
            if (jsonData.artifacts.length > 0) {
                output = 'Last successful ' + job + ' build will be loaded.';
                let buildUrl = host + '/job/' + encodeURIComponent(job) + '/lastSuccessfulBuild/artifact/';
                jsonResponse = {
                    "artifact" : {
                            "name": jsonData.artifacts[0].fileName, 
                            "url": buildUrl + jsonData.artifacts[1].relativePath
                        }
                    };
            } else {
                output = 'No available artifacts for ' + job;

            }
            response.send(JSON.stringify({ 'data':jsonResponse, 'speech': output, 'displayText': output }));
            resolve(output)
        } else {
            let output = 'Error while getting jobs list!';
            resolve(output);
        }
    });

  });
}

function callRestart () {
  return new Promise((resolve, reject) => {

    let path = '/quietDown';
    var url = 'http://' + username + ':' + password + '@' + host + path;
    
    console.log('API Request: ' + url);

    request.post({url: url}, function (err, res, body) {

      if (err) {
        let output = 'Error while restarting Jenkins server!';
        resolve(output)
        return console.error('restarting failed :', err);
      }
      console.log('Jenkins server will be restarted!', body);
      
      let output = 'Jenkins server will be restarted!';
      resolve(output)
    });
  });
}
