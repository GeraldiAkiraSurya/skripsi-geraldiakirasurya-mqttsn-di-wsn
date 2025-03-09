import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class RandomTest {
    DataConnection conn; //For sending message to Preon32
    BufferedOutputStream out;
    BufferedInputStream in; //For receiving message from Preon32
    MqttsnAssembler ma;
    MqttsnDisassembler md;

    public static void main(String[] args) {
        new RandomTest().run();
    }

    public void run() {
        Preon32Helper nodeHelper = null;
        try {
            nodeHelper = new Preon32Helper("COM6", 115200);
            //DataConnection conn = nodeHelper.runModule("bsv2");
            //BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            conn = nodeHelper.runModule("mqttgw");
            out = new BufferedOutputStream(conn.getOutputStream());
            in = new BufferedInputStream(conn.getInputStream());
//            receiveFromPreon32();
            runSendProg();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ma = new MqttsnAssembler();
        md = new MqttsnDisassembler();
    }

    private void runSendProg() {
        while(true) {
            byte[] mqttsnMsg = ma.createGWINFO((byte) 0x01);
            byte[] wirelessNodeId = new byte[] {(byte) 0x00, (byte) 0x01};
            byte[] encapsulatedMsg = ma.encapsulateMessage(wirelessNodeId,mqttsnMsg);

            sendToForwarder(encapsulatedMsg);
        }
    }

    public void sendToForwarder(byte[] b) {
        byte[] buffer = new byte[1024];

        System.arraycopy(b, 0, buffer, 0, b.length);

        try {
            conn.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
