import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class MqttsnGateway {

    private String brokerUrl = "tcp://broker.emqx.io:1883";
    private byte gatewayId = (byte) 0xCC;

    private String[] wirelessNodeIdCollection = {
            "node1", "node2", "node3",
            "node4", "node5", "node6"
    };

    private HashMap<String, Mqttv5Connection> mqttConnectionMap;
    private HashMap<String, String> topicIdMap;

    DataConnection conn;
    BufferedOutputStream out; //For sending message to Preon32
    BufferedInputStream in; //For receiving message from Preon32

    /*
    // For testing
    int lastSequenceNumber = -1;
    int totalPacketLost = 0;
     */

    public static void main(String[] args) {
        new MqttsnGateway().run();
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
            byte[] topicId = new byte[2];
            topicId[0] = (byte)(i & 0xFF);
            topicId[1] = (byte)(0x01);
            topicName = "skripsimqttsn/wsn/node" + i + "/all";

            String topicIdHex = DataTypeConverter.bytesToHex(topicId);
            topicIdMap.put(topicIdHex, topicName);
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
                while (true) {
                    byte[] buffer = new byte[128];

                    try {
                        in.read(buffer);

                        boolean isMQTTSN = (buffer[1] == (byte) 0xFE) ? true : false;

                        if (isMQTTSN) {
                            System.out.println("---------Received a message from MQTT-SN Forwarder!!!---------");
                            //System.out.println("isMQTTSN: " + isMQTTSN);
                            // Handle the received message from forwarder
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

        //System.out.println("Received Encapsulated Message (hex)\t: " + DataTypeConverter.bytesToHex(encapsulatedMessage));

        int length = encapsulatedMessage[0] & 0xFF;
        String wirelessNodeId = new String(encapsulatedMessage, 3, length-3, StandardCharsets.US_ASCII);
        System.out.println("Source (wirelessNodeId)\t: " + wirelessNodeId);
        byte[] mqttsnMessage = md.removeEncapsulation(encapsulatedMessage);
        //System.out.println("Decapsulated mqttsnMessage (hex)\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));

        byte messageType = md.getMessageType(mqttsnMessage);

        byte[] newMqttsnMessage;
        if (messageType == (byte) 0x01) {
            System.out.println("Gateway received a SEARCHGW message");
            newMqttsnMessage = ma.createGWINFO(gatewayId);
            sendToForwarder(newMqttsnMessage, wirelessNodeId);
        } else if (messageType == (byte) 0x04) {
            System.out.println("Gateway received a CONNECT message");
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

            byte returnCode = (connection.isConnected()) ? (byte) 0x00 : (byte) 0x01;
            newMqttsnMessage = ma.createCONNACK(returnCode);
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

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            String datetime = dtf.format(now);

            String jsonString = "{\"datetime\":\"" + datetime + "\", \"nodeId\":\"" + wirelessNodeId + "\", " + new String(data) + "}";
            connection.publish(topic, jsonString.getBytes());

            /*
            //For testing
            System.out.println("Gateway received a PUBLISH message");
            byte[] messageVariablePart = md.getMessageVariablePart(mqttsnMessage);
            byte[] data = new byte[messageVariablePart.length - 5];
            System.arraycopy(messageVariablePart, 5, data, 0, data.length);

            int sequenceNumber = Integer.parseInt(new String(data));
            System.out.println("Sequence Number: " + sequenceNumber);

            totalPacketLost += sequenceNumber-lastSequenceNumber-1;
            lastSequenceNumber = sequenceNumber;
            System.out.println("Total Packet Lost: " + totalPacketLost);

             */
        }
    }

    private void sendToForwarder(byte[] mqttsnMessage, String wirelessNodeId) {
        try {
            MqttsnAssembler ma = new MqttsnAssembler();
            byte[] encapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), mqttsnMessage);

            System.out.println("----------------Sending to Forwarder----------------");
            System.out.println("New mqttsn message\t\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));
            System.out.println("New encapsulated message\t\t: " + DataTypeConverter.bytesToHex(encapsulatedMessage));
            System.out.println("New encapsulated message length\t: " + encapsulatedMessage.length);
            byte[] b = new byte[128];
            System.arraycopy(encapsulatedMessage, 0, b, 0, encapsulatedMessage.length);

            out.write(b);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}