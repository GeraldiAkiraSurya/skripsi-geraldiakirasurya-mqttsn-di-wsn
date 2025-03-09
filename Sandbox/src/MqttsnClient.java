import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

import java.io.IOException;

public class MqttsnClient {
    private final int panID = 0xABCD;

    private int[] nodeAddrs = {0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006};
    private int nodeNumber = 2;
    private int localAddr = nodeAddrs[nodeNumber-1];
    private String clientId = "node" + nodeNumber;

    private byte gatewayId;
    private int gatewayAddr;

    private boolean isConnected;

    private AT86RF231 transceiver;
    private FrameIO fio;
    private Preon32Sensor sensor;

    public static void main(String[] args) {
        new MqttsnClient().run();
    }

    private void run() {
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
                        handleReceivedMqttsnMessage(frame);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void runMQTTSN() {
        isConnected = false;

        MqttsnAssembler ma = new MqttsnAssembler();
        byte[] mqttsnMessage = new byte[0];

        int sequenceNumber = 0;
        while(true) {
            if (gatewayAddr == 0) {
                mqttsnMessage = ma.createSEARCHGW(1);
                transmit(mqttsnMessage, 0xFFFF);
                //transmit(mqttsnMessage, 0x0005);
            } else if (!isConnected) {
                mqttsnMessage = ma.createCONNECT(false, true, clientId);
                transmit(mqttsnMessage, gatewayAddr);
            } else if (isConnected) {
                byte dup = (byte) 0x00;
                byte qos = (byte) 0x00;
                byte retain = (byte) 0x00;
                byte[] topicIdType = {0x00, 0x00};
                byte[] topicId = {(byte) (nodeNumber & 0xFF), 0x01};
                sensor.senseAll();
                //String data = sensor.getAllValue();
                String data = Integer.toString(sequenceNumber);
                sequenceNumber++;
                mqttsnMessage = ma.createPUBLISH(dup, qos, retain, topicIdType, topicId, data);
                transmit(mqttsnMessage, gatewayAddr);
            }
        }
    }

    private void handleReceivedMqttsnMessage(Frame frame) {
        MqttsnDisassembler md = new MqttsnDisassembler();

        byte[] mqttsnMessage = frame.getPayload();
        long senderAddr = frame.getSrcAddr();

        byte msgType = md.getMessageType(mqttsnMessage);
        byte[] msgVariablePart = md.getMessageVariablePart(mqttsnMessage);

        if (msgType == (byte) 0x02) {
            gatewayId = msgVariablePart[0];
            gatewayAddr = (int) senderAddr;
        } else if (msgType == (byte) 0x05) {
            byte returnCode = msgVariablePart[0];
            if (returnCode == (byte) 0x00) {
                isConnected = true;
            }
        }
    }


    private void transmit(byte[] mqttsnMessage, int destAddr) {
        int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

        final Frame testFrame = new Frame(frameControl);
        testFrame.setDestPanId(panID);
        testFrame.setDestAddr(destAddr);
        testFrame.setSrcAddr(localAddr);
        testFrame.setPayload(mqttsnMessage);

        try {
            fio.transmit(testFrame);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
