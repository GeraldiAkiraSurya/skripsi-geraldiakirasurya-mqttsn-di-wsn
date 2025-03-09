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
import java.io.InputStream;
import java.io.OutputStream;

public class MqttsnForwarderDebug {
    private USART usart;
    private InputStream in;
    private OutputStream out;

    private AT86RF231 transceiver;
    private FrameIO fio;

    private final int panID = 0xABCD;
    private int[] nodeAddrs = {0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006};
    private String[] listOfWirelessNodeId = {
            "node1", "node2", "node3",
            "node4", "node5", "node6"
    };
    private int nodeNumber = 5; //gateway di set sebagai node ke 0
    private final int localAddr = nodeAddrs[nodeNumber-1];

    public static void main(String[] args) {
        new MqttsnForwarderDebug().run();
    }

    private void run() {
        useUSART();
        setupRadio();
        runRadioReceiver();
		runUSARTReceiver();
    }

    public void useUSART() {
        usart = configUSART();
        try {
            out = usart.getOutputStream();
//            in = usart.getInputStream();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        runUSARTReceiver();
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void handleReceivedMqttsnMessage(Frame frame) {
//        System.out.println("==========BEGIN HANDLING RECEIVED MQTTSN MESSAGE==========");
//        System.out.println("Received a frame!!!");
//        System.out.println(frame);

        byte[] mqttsnMessage = frame.getPayload();
        int senderAddr = (int) frame.getSrcAddr();
        //String msg = payloadToString(frame);

//        System.out.println("Frame from\t\t: " + senderAddr);
//						for (int i = 0; i < dg.length; i++) {
//							System.out.print(dg[i]);
//						}
//        System.out.println("Message in hex\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));

        MqttsnAssembler ma = new MqttsnAssembler();

        String wirelessNodeId = "";
        for (int i = 0; i < nodeAddrs.length; i++) {
            if (senderAddr == nodeAddrs[i]) wirelessNodeId = listOfWirelessNodeId[i];
        }
        byte[] encapsulatedMessage = ma.encapsulateMessage(wirelessNodeId.getBytes(), mqttsnMessage);

//        System.out.println("Encapsulated Message in hex: " + DataTypeConverter.bytesToHex(encapsulatedMessage));

        sendToPC(encapsulatedMessage);
    }

    private void sendToPC(byte[] packet) {
        try {
//						out.write(str.getBytes("UTF-8"), 0, str.length());
//						usart.flush();
//						out.write(str.getBytes(), 0, str.length());
//						usart.flush();
            //usart.flush();
//            out.write((byte) 0xAB);
//            out.write((byte) 0xCD);
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

    private void sendgwinfo(final long destAddr) {
        new Thread() {
            public void run() {
                String message = "gwinfo:1";

                int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                        | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

                final Frame testFrame = new Frame(frameControl);
                testFrame.setDestPanId(panID);
                testFrame.setDestAddr(destAddr);
                testFrame.setSrcAddr(localAddr);
                testFrame.setPayload(message.getBytes());

                try {
                    fio.transmit(testFrame);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void runUSARTReceiver() {
        new Thread() {
            public void run() {
                while (true) {
//                    try {
//                        if (!(usart.available() > 0)) break;
//                    } catch (USARTException e) {
//                        e.printStackTrace();
//                    }
                    String packet;
                    String msg;
                    byte[] buffer = new byte[128];

                    try {
//                        usart.flush();
//                        System.out.println("usart.available : " + usart.available());
                        int num = usart.readFully(buffer);
//                        usart.flush();
//                        int num = usart.readFully(buffer, 0, 1024);
//                        byte[] b = new byte[] {(byte) (num & 0xFF)};
//                        byte[] b2 = BigInteger.valueOf(num).toByteArray();

//                        send(buffer, 0x0002);

//                        in.read(buffer);
//                        send(buffer, 0x0002);

                        boolean isMQTTSN = (buffer[1] == (byte) 0xFE) ? true : false;

//                        int encapsulationLength = buffer[0] & 0xFF;
//                        String wirelessNodeId = new String(buffer, 3, encapsulationLength-3);

//                        send(wirelessNodeId.getBytes(), 0x0002);
                        if (isMQTTSN) {
//                            System.out.println("Got message from MQTT-SN Gateway!!!");
//                            System.out.println("Buffer[0] = " + DataTypeConverter.byteToHex(buffer[0]));
//                            System.out.println("Buffer[1] = " + DataTypeConverter.byteToHex(buffer[1]));
//                            System.out.println("isMQTTSN: " + isMQTTSN);
                            handleReceivedEncapsulatedMessage(buffer);
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void send(byte[] b, long destAddr) {
        int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

        final Frame testFrame = new Frame(frameControl);
        testFrame.setDestPanId(panID);
        testFrame.setDestAddr(destAddr);
        testFrame.setSrcAddr(localAddr);
        testFrame.setPayload(b);


        try {
            fio.transmit(testFrame);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleReceivedEncapsulatedMessage(byte[] encapsulatedMessage) {
        MqttsnAssembler ma = new MqttsnAssembler();
        MqttsnDisassembler md = new MqttsnDisassembler();

//        System.out.println("encapsulatedMessage in hex: " + DataTypeConverter.bytesToHex(encapsulatedMessage));

        int encapsulationLength = encapsulatedMessage[0] & 0xFF;
        String wirelessNodeId = new String(encapsulatedMessage, 3, encapsulationLength-3);
//        System.out.println("wirelessNodeId: " + wirelessNodeId);

        int destAddr = 0;
        for (int i = 0; i < listOfWirelessNodeId.length; i++) {
            if (wirelessNodeId.equals(listOfWirelessNodeId[i])) destAddr = i+1;
        }

//        int destAddr = 0x0002;

        byte[] mqttsnMessage = md.removeEncapsulation(encapsulatedMessage);
//        System.out.println("mqttsnMessage in hex: " + DataTypeConverter.bytesToHex(mqttsnMessage));

        byte messageType = md.getMessageType(mqttsnMessage);

//        transmit(mqttsnMessage, destAddr);
//        if (messageType == (byte) 0x02) {
            //GWINFO
            transmit(mqttsnMessage, destAddr);
//        }

    }

    private void transmit(byte[] mqttsnMessage, int destAddr) {

//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

        MqttsnDisassembler md = new MqttsnDisassembler();

//        System.out.println("--------------BEGIN TRANSMIT FUNCTION--------------");

        int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

        final Frame testFrame = new Frame(frameControl);
        testFrame.setDestPanId(panID);
        testFrame.setDestAddr(destAddr);
        testFrame.setSrcAddr(localAddr);
        testFrame.setPayload(mqttsnMessage);


        try {

//            System.out.println("Will transmit a frame!!!");
//            System.out.println("Message type\t: " + md.getMessageType(mqttsnMessage));
//            System.out.println("Payload field length\t: " + md.getMessageVariablePart(mqttsnMessage).length);
//            System.out.println("Header field length\t: " + md.getMessageHeader(mqttsnMessage).length);
//            System.out.println("Total pakcet length\t: " + mqttsnMessage.length);
//            System.out.println("Message in hex\t: " + DataTypeConverter.bytesToHex(mqttsnMessage));
            fio.transmit(testFrame);
//            System.out.println("Frame transmitted!!!");

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

/*
    private void transmit(final String command, int destAddr) {

//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

        System.out.println("--------------BEGIN TRANSMIT FUNCTION--------------");

//		new Thread() {
//			public void run() {
        byte[] headerField = new byte[0];
        byte[] payloadField = new byte[0];
        //byte[] packet = new byte[0];

        MQTTSNMessage mqttsnMessage = new MQTTSNMessage();

        if (command.equals("gwinfo")) {
            mqttsnMessage.createGWINFO((byte) localAddr);
        } else if (command.equals("connack")) {
            mqttsnMessage.createCONNACK((byte) 0x00);
        }

        int frameControl = Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
                | Frame.INTRA_PAN | Frame.SRC_ADDR_16;

        final Frame testFrame = new Frame(frameControl);
        testFrame.setDestPanId(panID);
        testFrame.setDestAddr(destAddr);
        testFrame.setSrcAddr(localAddr);
//				testFrame.setPayload(message.getBytes());
        testFrame.setPayload(mqttsnMessage.getMessage());


        try {

            System.out.println("Will transmit a frame!!!");
            System.out.println("Message type\t: " + command);
            System.out.println("Payload field length\t: " + mqttsnMessage.getMessageVariablePart().length);
            System.out.println("Header field length\t: " + mqttsnMessage.getMessageHeader().length);
            System.out.println("Total pakcet length\t: " + mqttsnMessage.getMessage().length);
            System.out.println("Message in hex\t: " + DataTypeConverter.bytesToHex(mqttsnMessage.getMessage()));
            fio.transmit(testFrame);
            System.out.println("Frame transmitted!!!");

        } catch (IOException e) {
            e.printStackTrace();
        }
//			}
//		}.start();

    }

*/