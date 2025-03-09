import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

import java.io.IOException;

public class MqttsnClientDebug {
    private final int panID = 0xABCD;

    private int[] nodeAddrs = {0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006};
    private int nodeNumber = 2; // isi dengan index 2-6 saja, karena index 1 dipakai gateway
    private int localAddr = nodeAddrs[nodeNumber-1];
    private String clientId = "node" + nodeNumber;

    private byte gatewayId;
    private int gatewayAddr;

    private boolean isConnected;
    //private boolean waitingPubAck;

    private AT86RF231 transceiver;
    private FrameIO fio;
    private Preon32Sensor sensor;

    public static void main(String[] args) {
        new MqttsnClientDebug().run();
    }

    private void run() {
        System.out.println("This is " + clientId);
        System.out.println("Local address:" + localAddr);

        sensor = new Preon32Sensor();
        sensor.init();
        setupRadio();
        runRadioReceiver();
        runMQTTSN();
    }

    private void setupRadio() {
        try {
            transceiver = Node.getInstance().getTransceiver();
            transceiver.open();
            transceiver.setAddressFilter(panID, localAddr, localAddr, false);
            RadioDriver radioDriver = new AT86RF231RadioDriver(transceiver);
            fio = new RadioDriverFrameIO(radioDriver);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void runRadioReceiver() {
        new Thread() {
            public void run() {
                Frame frame = new Frame();
                while (true) {
                    try {
                        fio.receive(frame);
//						System.out.println(frame);

                        handleReceivedMqttsnMessage(frame);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void runMQTTSN() {
//		new Thread() {
//			public void run() {
        isConnected = false;
        boolean waitingPubAck = false;

        MqttsnAssembler ma = new MqttsnAssembler();
        byte[] mqttsnMessage = new byte[0];
        while(true) {
            if (gatewayAddr == 0) {
                //broadcast("searchgw");
                mqttsnMessage = ma.createSEARCHGW(1);
                transmit(mqttsnMessage, 0xFFFF);
            } else if (!isConnected) {
                mqttsnMessage = ma.createCONNECT(false, true, clientId);
                transmit(mqttsnMessage, gatewayAddr);
            } else if (isConnected && !waitingPubAck) {
                byte dup = (byte) 0x00;
                byte qos = (byte) 0x00;
                byte retain = (byte) 0x00;
                byte[] topicIdType = {0x00, 0x00};
                byte[] topicId = {(byte) (nodeNumber & 0xFF), 0x01};
                sensor.senseAll();
                String data = sensor.getAllValue();
                mqttsnMessage = ma.createPUBLISH(dup, qos, retain, topicIdType, topicId, data);
                transmit(mqttsnMessage, gatewayAddr);
            }

//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void handleReceivedMqttsnMessage(Frame frame) {
        MqttsnDisassembler md = new MqttsnDisassembler();

        System.out.println("==========BEGIN HANDLING RECEIVED MQTTSN MESSAGE==========");
        System.out.println("Received a frame!!!");
        System.out.println(frame);

        byte[] mqttsnMessage = frame.getPayload();
        long senderAddr = frame.getSrcAddr();

        System.out.println("Frame from\t\t: " + senderAddr);
        System.out.println("Message in hex\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));

        byte msgType = md.getMessageType(mqttsnMessage);
        byte[] msgVariablePart = md.getMessageVariablePart(mqttsnMessage);

        if (msgType == (byte) 0x02) {
            System.out.println("The received frame is a GWINFO message!");
            if (msgVariablePart.length == 1) { //Ga ada field GwAdd
                gatewayAddr = (int) senderAddr;
            } else {
                gatewayAddr = msgVariablePart[1];
            }
            gatewayId = msgVariablePart[0];
            System.out.println("Gateway address updated.");
            System.out.println("Current GW address\t: " + gatewayAddr);
        } else if (msgType == (byte) 0x05) {
            System.out.println("The received frame is a CONNACK message!");
            byte returnCode = msgVariablePart[0];
//            System.out.println("msgVariablePart: " + DataTypeConverter.bytesToHex(msgVariablePart));
            System.out.println("Return Code: " + DataTypeConverter.byteToHex(returnCode));
            if (returnCode == (byte) 0x00) {
                isConnected = true;
                System.out.println("Connection to gateway is established!");
            }
        }
    }


    private void transmit(byte[] mqttsnMessage, int destAddr) {
//		new Thread() {
//			public void run() {
        MqttsnAssembler ma = new MqttsnAssembler();
        MqttsnDisassembler md = new MqttsnDisassembler();

        System.out.println("--------------BEGIN TRANSMIT PROCESS--------------");
//        System.out.println("Command: " + command);
        byte messageType = md.getMessageType(mqttsnMessage);
//        System.out.println("Message type\t: " + DataTypeConverter.byteToHex(messageType));
        String command = "";

        if (messageType == (byte) 0x01) {
            command = "SEARCHGW";
        } else if (messageType == (byte) 0x04) {
            command = "CONNECT";
        } else if (messageType == (byte) 0x0C) {
            command = "PUBLISH";
        }

        int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

        final Frame testFrame = new Frame(frameControl);
        testFrame.setDestPanId(panID);
        testFrame.setDestAddr(destAddr);
        testFrame.setSrcAddr(localAddr);
        testFrame.setPayload(mqttsnMessage);

        try {

            System.out.println("Will transmit a frame!!!");
            System.out.println("Message type (hex)\t: " + DataTypeConverter.byteToHex(messageType));
            System.out.println("Message type (str)\t: " + command);
            System.out.println("Payload field length\t: " + md.getMessageVariablePart(mqttsnMessage).length);
            System.out.println("Header field length\t: " + md.getMessageHeader(mqttsnMessage).length);
            System.out.println("Total pakcet length\t: " + mqttsnMessage.length);
            System.out.println("Message in hex\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));
            fio.transmit(testFrame);
            System.out.println("Frame transmitted!!!");

        } catch (IOException e) {
            e.printStackTrace();
        }

//			}
//		}.start();

    }

    private String payloadToString(Frame frame) {
        byte[] dg = frame.getPayload();
//		long senderAddr = frame.getSrcAddr();
        String str = new String(dg, 0, dg.length);

        return str;
    }
}
