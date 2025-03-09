import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class MqttsnGatewayDebug {

    private String brokerUrl = "tcp://broker.emqx.io:1883";
    private byte gatewayId = (byte) 0xCC;

    private String[] wirelessNodeIdCollection = {
            "node1", "node2", "node3",
            "node4", "node5", "node6"
    };
    private HashMap<String, Mqttv5Connection> mqttConnectionMap;

//    private String[] topicCollection = {
//            "skripsimqttsn/node1/allsensor", "skripsimqttsn/node2/allsensor",
//            "skripsimqttsn/node3/allsensor", "skripsimqttsn/node4/allsensor",
//            "skripsimqttsn/node5/allsensor", "skripsimqttsn/node6/allsensor",
//    };

    private HashMap<String, String> topicIdMap;

    //private Mqttv5Connection[] mqttConnections = new Mqttv5Connection[6];

    DataConnection conn; //For sending message to Preon32
    BufferedOutputStream out;
    BufferedInputStream in; //For receiving message from Preon32
    //OutputStream out;
    //InputStream in;


    public static void main(String[] args) {
        new MqttsnGatewayDebug().run();
    }

    public void run() {
        setupTopicIdMap();
        setupMqttConnectionMap();
        runPreonModule();
        runInputStream();
    }

    private void setupTopicIdMap() {
        topicIdMap = new HashMap<String, String>();
        String topicName = "";
        for (int i = 1; i <= wirelessNodeIdCollection.length; i++) {
            byte[] tempTopicId = new byte[2];
            tempTopicId[0] = (byte)(i & 0xFF);
            String tempTopicName = "skripsimqttsn/wsn/node" + i;
            for (int j = 1; j <= 5; j++) {
                int temp = j;
                tempTopicId[1] = (byte)(temp & 0xFF);
                //System.out.println("topicId[] : " + DataTypeConverter.bytesToHex(topicId));
                topicName = tempTopicName;
                switch (j) {
                    case 1:
                        topicName += "/all";
                        break;
                    case 2:
                        topicName += "/temperature";
                        break;
                    case 3:
                        topicName += "/accelerometer";
                    case 4:
                        topicName += "/pressure";
                        break;
                    case 5:
                        topicName += "/humidity";
                        break;
                }
                byte[] topicId = new byte[2];
                topicId[0] = tempTopicId[0];
                topicId[1] = tempTopicId[1];

                String topicIdHex = DataTypeConverter.bytesToHex(topicId);
                topicIdMap.put(topicIdHex, topicName);
            }
        }

        System.out.println("==Topic Id Map==");
        for (String s : topicIdMap.keySet()) {
            System.out.println("TopicId: " + s + ", Value: " + topicIdMap.get(s));
        }
    }

    private void setupMqttConnectionMap() {
        mqttConnectionMap = new HashMap<String, Mqttv5Connection>();
        for (String wirelessNodeId: wirelessNodeIdCollection) {
            mqttConnectionMap.put(wirelessNodeId, new Mqttv5Connection(brokerUrl));
        }
    }

    private void runPreonModule() {
        Preon32Helper nodeHelper = null;
        try {
            nodeHelper = new Preon32Helper("COM6", 115200);
            //DataConnection conn = nodeHelper.runModule("bsv2");
            //BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            conn = nodeHelper.runModule("mqttsnforwarder");
            out = new BufferedOutputStream(conn.getOutputStream());
            in = new BufferedInputStream(conn.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runInputStream() {
        new Thread() {
            public void run() {
                // bikin buffer buat nyimpen data dr preon32
                String s;
                String s2;
                while (true) {
                    byte[] buffer = new byte[128];

                    try {
                        in.read(buffer);

                        //System.out.println(DataTypeConverter.bytesToHex(buffer));

                        boolean isMQTTSN = (buffer[1] == (byte) 0xFE) ? true : false;

                        if (isMQTTSN) {
                            System.out.println("---------Received a message from MQTT-SN Forwarder!!!---------");
                            //System.out.println("Buffer[0] = " + DataTypeConverter.byteToHex(buffer[0]));
                            //System.out.println("Buffer[1] = " + DataTypeConverter.byteToHex(buffer[1]));
                            System.out.println("isMQTTSN: " + isMQTTSN);
                            handleReceivedEncapsulatedMessage(buffer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void handleReceivedEncapsulatedMessage(byte[] encapsulatedMessage) {
        MqttsnAssembler ma = new MqttsnAssembler();
        MqttsnDisassembler md = new MqttsnDisassembler();

        System.out.println("Received encapsulatedMessage (hex)\t: " + DataTypeConverter.bytesToHex(encapsulatedMessage));

        int length = encapsulatedMessage[0] & 0xFF;
        String wirelessNodeId = new String(encapsulatedMessage, 3, length-3, StandardCharsets.US_ASCII);
        System.out.println("Source (wirelessNodeId)\t: " + wirelessNodeId);
        byte[] mqttsnMessage = md.removeEncapsulation(encapsulatedMessage);
        System.out.println("Opened mqttsnMessage (hex)\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));

        byte messageType = md.getMessageType(mqttsnMessage);

        byte[] newMqttsnMessage;
        byte[] newEncapsulatedMessage;
        if (messageType == (byte) 0x01) {
            System.out.println("Gateway received a SEARCHGW message");
            newMqttsnMessage = ma.createGWINFO(gatewayId);
//            newEncapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), newMqttsnMessage);
//            System.out.println("newMqttsnMessage : " + DataTypeConverter.bytesToHex(newMqttsnMessage));
//            System.out.println("newEncapsulatedMessage : " + DataTypeConverter.bytesToHex(newEncapsulatedMessage));
//            sendToForwarder(newEncapsulatedMessage);
            sendToForwarder(newMqttsnMessage, wirelessNodeId);
        } else if (messageType == (byte) 0x04) {
            System.out.println("Gateway received a CONNECT message");
//            int index = getIndexOfAList(wirelessNodeId, wirelessNodeIdCollection);
            Mqttv5Connection connection = mqttConnectionMap.get(wirelessNodeId);
            if (connection.getClientId() == null) {
                byte[] messageVariablePart = md.getMessageVariablePart(mqttsnMessage);
                String clientId = new String(messageVariablePart, 4, messageVariablePart.length-4);
                connection.setClientId(clientId);
            }
            if (connection.getClient() == null) {
                connection.setupClient();
                connection.setConnectionOptions();
            }
            if (!connection.isConnected()) {
                connection.connect();
            }
//            if (mqttConnections[index] == null) {
//                byte[] messageVariablePart = md.getMessageVariablePart(mqttsnMessage);
//                String clientId = new String(messageVariablePart, 4, messageVariablePart.length-4);
//                System.out.println("The clientId is: " + clientId);
//                //Mqttv5Connection connection = new Mqttv5Connection(clientId);
//                mqttConnections[index] = new Mqttv5Connection(clientId);
//                mqttConnections[index].setupConnectionOptions();
//                mqttConnections[index].setupClient();
//                mqttConnections[index].runConnection();
//            }

            byte returnCode = (connection.isConnected()) ? (byte) 0x00 : (byte) 0x01;
            newMqttsnMessage = ma.createCONNACK(returnCode);
//            newEncapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), newMqttsnMessage);
//            sendToForwarder(newEncapsulatedMessage);
            sendToForwarder(newMqttsnMessage, wirelessNodeId);
        } else if (messageType == (byte) 0x0C) {
            System.out.println("Gateway received a PUBLISH message");
            Mqttv5Connection connection = mqttConnectionMap.get(wirelessNodeId);
            byte[] messageVariablePart = md.getMessageVariablePart(mqttsnMessage);
            System.out.println("messageVariablePart: " + DataTypeConverter.bytesToHex(messageVariablePart));

            byte[] topicId = {messageVariablePart[1], messageVariablePart[2]};
            String topicIdHex = DataTypeConverter.bytesToHex(topicId);
            System.out.println("Current TopicId : " + topicIdHex);
            String topic = topicIdMap.get(topicIdHex);

            // Set content for publishing to a topic
            byte[] data = new byte[messageVariablePart.length - 5];
            System.arraycopy(messageVariablePart, 5, data, 0, data.length);
            System.out.println("data array: " + DataTypeConverter.bytesToHex(data));

            //DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            String datetime = dtf.format(now);

            String jsonString = "{\"datetime\":\"" + datetime + "\", \"nodeId\":\"" + wirelessNodeId + "\", " + new String(data) + "}";
//            String jsonString = "{" + "nodeId:" + wirelessNodeId + ", " + new String(data) + "}";
            connection.publish(topic, jsonString.getBytes());
        }


//        if (messageType == (byte) 0x01) {
//            System.out.println("Gateway received a SEARCHGW message");
//            newMqttsnMessage = ma.createGWINFO(gatewayId);
//            newEncapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), newMqttsnMessage);
//            sendToForwarder(newEncapsulatedMessage);
//        } else if (messageType == (byte) 0x04) {
//            System.out.println("Gateway received a CONNECT message");
//            int index = getIndexOfAList(wirelessNodeId, wirelessNodeIdCollection);
//            if (mqttConnections[index] == null) {
//                byte[] messageVariablePart = md.getMessageVariablePart(mqttsnMessage);
//                String clientId = new String(messageVariablePart, 4, messageVariablePart.length-4);
//                System.out.println("The clientId is: " + clientId);
//                //Mqttv5Connection connection = new Mqttv5Connection(clientId);
//                mqttConnections[index] = new Mqttv5Connection(clientId);
//                mqttConnections[index].setupConnectionOptions();
//                mqttConnections[index].setupClient();
//                mqttConnections[index].runConnection();
//            }
//
//            newMqttsnMessage = ma.createCONNACK((byte) 0x00);
//            newEncapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), newMqttsnMessage);
//            sendToForwarder(newEncapsulatedMessage);
//        }
    }

    private void sendToForwarder(byte[] mqttsnMessage, String wirelessNodeId) {
//        new Thread() {
//            public void run() {
//        String s;
//        s = "This is PC";
        try {
//            conn.write(s.getBytes(), 0, s.length());
//            conn.flush();
//            conn.flush();
            MqttsnAssembler ma = new MqttsnAssembler();

            byte[] encapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), mqttsnMessage);

            System.out.println("----------------Sending to Forwarder----------------");
            System.out.println("New mqttsn message\t\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));
            System.out.println("New encapsulated message\t\t: " + DataTypeConverter.bytesToHex(encapsulatedMessage));
            System.out.println("New encapsulated message length\t: " + encapsulatedMessage.length);
            byte[] b = new byte[128];
            System.arraycopy(encapsulatedMessage, 0, b, 0, encapsulatedMessage.length);
//            for (int i = encapsulatedMessage.length; i < (1024 - encapsulatedMessage.length); i++) {
//                b[i] = (byte) 0x11;
//            }

//            b[11] = (byte) 0x00;

            out.write(b);
            out.flush();
//            conn.flush();
//            Thread.sleep(500);
//            conn.write(b, 0, 1024);
//            conn.flush();

//            byte[] b = new byte[] {0x05, 0x06};
//            conn.flush();
//            conn.write(b, 0, 2);
//            conn.flush();
            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
        }
//            }
//        }.start();
    }

    private int getIndexOfAList(String s, String[] arrayOfString) {
        int index = -1;
        for (int i=0;i<arrayOfString.length;i++) {
            if (arrayOfString[i].equals(s)) {
                index = i;
                return index;
            }
        }
        return index;
    }
}

/*
    private void sendToForwarder(byte[] encapsulatedMessage) {
//        new Thread() {
//            public void run() {
//        String s;
//        s = "This is PC";
        try {
//            conn.write(s.getBytes(), 0, s.length());
//            conn.flush();
//            conn.flush();

            System.out.println("----------------Sending to Forwarder----------------");
            System.out.println("New encapsulated message\t\t: " + DataTypeConverter.bytesToHex(encapsulatedMessage));
            System.out.println("New encapsulated message length\t: " + encapsulatedMessage.length);
            byte[] b = new byte[128];
            System.arraycopy(encapsulatedMessage, 0, b, 0, encapsulatedMessage.length);
//            for (int i = encapsulatedMessage.length; i < (1024 - encapsulatedMessage.length); i++) {
//                b[i] = (byte) 0x11;
//            }

//            b[11] = (byte) 0x00;

            out.write(b);
            out.flush();
//            conn.flush();
//            Thread.sleep(500);
//            conn.write(b, 0, 1024);
//            conn.flush();

//            byte[] b = new byte[] {0x05, 0x06};
//            conn.flush();
//            conn.write(b, 0, 2);
//            conn.flush();
            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
        }
//            }
//        }.start();
    }
    */