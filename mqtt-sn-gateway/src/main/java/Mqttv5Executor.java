import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class Mqttv5Executor {

    MqttAsyncClient v5Client;
    String clientId = "JavaSample";

    public Mqttv5Executor(String clientId) {
        this.clientId = clientId;
    }

    public void execute() {
        String topic        = "wsn/node1/temperature";
        String content      = "Message from MqttPublishSample";
        int qos             = 2;
        String broker       = "tcp://broker.emqx.io:1883";

        try {
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(true);

            MemoryPersistence persistence = new MemoryPersistence();
            this.v5Client = new MqttAsyncClient(broker, clientId, persistence);

            IMqttToken token = this.v5Client.connect(connOpts);
            token.waitForCompletion();
            System.out.println("Connected");
            System.out.println("Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            token = this.v5Client.publish(topic, message);
            token.waitForCompletion();
            System.out.println("Disconnected");
            System.out.println("Close client.");
            this.v5Client.close();
            System.exit(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
