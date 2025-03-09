import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class Mqttv5Connection {

    MqttAsyncClient v5Client;
    String clientId;
    int qos;
    String broker;

    MqttConnectionOptions connOpts;
    IMqttToken token;

    public Mqttv5Connection(String brokerUrl) {
        broker = brokerUrl;
        connOpts = new MqttConnectionOptions();
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setConnectionOptions() {
        connOpts.setCleanStart(true);
    }

    public void setupClient() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            v5Client = new MqttAsyncClient(broker, clientId, persistence);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            System.out.println("Connecting MQTTv5 client...");
            token = v5Client.connect(connOpts);
            token.waitForCompletion();
            System.out.println("Client " + clientId + " is connected.\n");
        } catch (MqttException e) {
            System.out.println("Error connecting to broker: " + e.getMessage());
        }
    }

    public void publish(String topic, byte[] content) {
        System.out.println("Publishing message for ClientID " + clientId);
        System.out.println("Topic: " + topic);
        System.out.println("Content: " + new String(content));
        try {
            MqttMessage message = new MqttMessage(content);
            message.setQos(qos);
            token = v5Client.publish(topic, message);
            token.waitForCompletion();
            System.out.println("Message published!\n");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MqttAsyncClient getClient() {
        return this.v5Client;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean isConnected() {
        return this.v5Client.isConnected();
    }
}
