import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTParams;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

import java.io.IOException;
import java.io.OutputStream;

public class MqttsnForwarder {
    private USART usart;
    private OutputStream out;

    private AT86RF231 transceiver;
    private FrameIO fio;

    private final int panID = 0xABCD;
    private int[] nodeAddrs = {0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006};
    private int nodeNumber = 5;
    private final int localAddr = nodeAddrs[nodeNumber-1];

    private String[] listOfWirelessNodeId = {
            "node1", "node2", "node3",
            "node4", "node5", "node6"
    };

    public static void main(String[] args) {
        new MqttsnForwarder().run();
    }

    private void run() {
        useUSART();
        setupRadio();
        runRadioReceiver();
		runUSARTReceiver();
    }

    private void useUSART() {
        usart = configUSART();
        try {
            out = usart.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static USART configUSART() {
        int instanceID = 0;
        USARTParams params = new USARTParams(115200, USART.DATA_BITS_8,
                USART.STOP_BITS_1, USART.PARITY_NONE);
        NativeUSART usart = NativeUSART.getInstance(instanceID);
        try {
            usart.close();
            usart.open(params);

            return usart;
        } catch (Exception e) {
            return null;
        }
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
                        handleReceivedMqttsnMessage(frame);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void handleReceivedMqttsnMessage(Frame frame) {
        byte[] mqttsnMessage = frame.getPayload();
        int senderAddr = (int) frame.getSrcAddr();

        MqttsnAssembler ma = new MqttsnAssembler();

        String wirelessNodeId = "";
        for (int i = 0; i < nodeAddrs.length; i++) {
            if (senderAddr == nodeAddrs[i]) wirelessNodeId = listOfWirelessNodeId[i];
        }
        byte[] encapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), mqttsnMessage);

        sendToPC(encapsulatedMessage);
    }

    private void sendToPC(byte[] packet) {
        try {
            out.flush();
            byte[] b = new byte[128];
            System.arraycopy(packet, 0, b, 0, packet.length);
            out.write(b);
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void runUSARTReceiver() {
        new Thread() {
            public void run() {
                while (true) {
                    byte[] buffer = new byte[128];

                    try {
                        int num = usart.readFully(buffer);

                        boolean isMQTTSN = (buffer[1] == (byte) 0xFE) ? true : false;

                        if (isMQTTSN) {
                            handleReceivedEncapsulatedMessage(buffer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void handleReceivedEncapsulatedMessage(byte[] encapsulatedMessage) {
        MqttsnAssembler ma = new MqttsnAssembler();
        MqttsnDisassembler md = new MqttsnDisassembler();

        int encapsulationLength = encapsulatedMessage[0] & 0xFF;
        String wirelessNodeId = new String(encapsulatedMessage, 3, encapsulationLength-3);

        int destAddr = 0;
        for (int i = 0; i < listOfWirelessNodeId.length; i++) {
            if (wirelessNodeId.equals(listOfWirelessNodeId[i])) destAddr = i+1;
        }

        byte[] mqttsnMessage = md.removeEncapsulation(encapsulatedMessage);
        transmit(mqttsnMessage, destAddr);
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