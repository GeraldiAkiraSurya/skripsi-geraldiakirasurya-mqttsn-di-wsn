var sense1 = [];
var sense2 = [];
var sense3 = [];
var sense4 = [];
var sense5 = [];

// Create a client instance
//client = new Paho.MQTT.Client(location.hostname, Number(location.port), "clientId");
client1 = new Paho.MQTT.Client("broker.emqx.io", Number(8083), "monitorPreon32");

// set callback handlers
client1.onConnectionLost = onConnectionLost;
client1.onMessageArrived = onMessageArrived;

// connect the client
client1.connect({onSuccess:onConnect});

// called when the client connects
function onConnect() {
  // Once a connection has been made, make a subscription and send a message.
  console.log("onConnect");
  client1.subscribe("skripsimqttsn/wsn/#");
  // message = new Paho.MQTT.Message("Hello");
  // message.destinationName = "World";
  // client1.send(message);
}

// called when the client loses its connection
function onConnectionLost(responseObject) {
  if (responseObject.errorCode !== 0) {
    console.log("onConnectionLost:"+responseObject.errorMessage);
  }
}

// called when a message arrives
function onMessageArrived(message) {
  console.log("onMessageArrived:"+message.payloadString);
  let messageStr = message.payloadString;
  // let jsonString = JSON.stringify(messageStr);
  // console.log(jsonString);
  // console.log(jsonString);
  // console.log(message.canApprove);
  let object = JSON.parse(messageStr);
  // let object = JSON.parse(jsonString);
  let nodeId = object.nodeId;
  let tValue = object.t;
  let aValue = object.a;
  let pValue = object.p;
  let hValue = object.h;

  // console.log(object);

  if (nodeId == "node1") {
    document.getElementById('temperature1').innerHTML = tValue + " &deg;C";
    document.getElementById('pressure1').innerHTML = pValue;
    document.getElementById('humidity1').innerHTML = hValue + " %";
    document.getElementById('acceleration1').innerHTML = "[" + aValue + "]";
  }

  if (nodeId == "node2") {
    document.getElementById('temperature2').innerHTML = tValue + " &deg;C";
    document.getElementById('pressure2').innerHTML = pValue;
    document.getElementById('humidity2').innerHTML = hValue + " %";
    document.getElementById('acceleration2').innerHTML = "[" + aValue + "]";
  }

  if (nodeId == "node3") {
    document.getElementById('temperature3').innerHTML = tValue + " &deg;C";
    document.getElementById('pressure3').innerHTML = pValue;
    document.getElementById('humidity3').innerHTML = hValue + " %";
    document.getElementById('acceleration3').innerHTML = "[" + aValue + "]";
  }

  if (nodeId == "node4") {
    document.getElementById('temperature4').innerHTML = tValue + " &deg;C";
    document.getElementById('pressure4').innerHTML = pValue;
    document.getElementById('humidity4').innerHTML = hValue + " %";
    document.getElementById('acceleration4').innerHTML = "[" + aValue + "]";
  }
}