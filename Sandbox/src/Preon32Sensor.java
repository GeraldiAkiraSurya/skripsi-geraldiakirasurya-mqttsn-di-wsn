import com.virtenio.driver.device.ADT7410;
import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.device.MPL115A2;
import com.virtenio.driver.device.SHT21;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.I2CException;
import com.virtenio.driver.i2c.NativeI2C;
import com.virtenio.driver.spi.NativeSPI;

import java.util.Arrays;

public class Preon32Sensor {

    private ADT7410 temperatureSensor;
    private float temperatureValue;

    private ADXL345 accelerationSensor;
    private short[] accelValue = new short[3];

    private MPL115A2 pressureSensor;
    private float pressureValue;

    private SHT21 humiditySensor;
    private float humidityValue;

    public void init() {
        try {
            NativeI2C i2c = NativeI2C.getInstance(1);
            i2c.open(I2C.DATA_RATE_400);

            //Setup temperature sensor
            temperatureSensor = new ADT7410(i2c, ADT7410.ADDR_0, null, null);
            if (!temperatureSensor.isOpened()) {
                temperatureSensor.open();
                temperatureSensor.setMode(ADT7410.CONFIG_MODE_ONE_SHOT);
            }

            //Setup acceleration sensor
            GPIO accelCs = NativeGPIO.getInstance(20);
            NativeSPI spi = NativeSPI.getInstance(0);
            spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);

            accelerationSensor = new ADXL345(spi, accelCs);
            accelerationSensor.open();
            accelerationSensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
            accelerationSensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
            accelerationSensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);

            //Setup pressure sensor
            GPIO resetPin = NativeGPIO.getInstance(24);
            GPIO shutDownPin = NativeGPIO.getInstance(12);
            pressureSensor = new MPL115A2(i2c, resetPin, shutDownPin);
            pressureSensor.open();
            pressureSensor.setReset(false);
            pressureSensor.setShutdown(false);

            //Setup humidity sensor
            humiditySensor = new SHT21(i2c);
            humiditySensor.open();
            humiditySensor.setResolution(SHT21.RESOLUTION_RH12_T14);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getAllValue() {
        String result = "\"t\":" + getTemperatureValue() +
                ", \"a\":" + Arrays.toString(getAccelValue()) +
                ", \"p\":" + getPressureValue() +
                ", \"h\":" + getHumidityValue();

        return result;
    }

    private double getTemperatureValue() {
        return (Math.round(temperatureValue * 100.0) / 100.0);
    }

    private short[] getAccelValue() {
        return this.accelValue;
    }

    private double getPressureValue() {
        return (Math.round(pressureValue * 100.0) / 100.0);
    }

    private int getHumidityValue() {
        return (int) (Math.round(pressureValue));
    }

    public void senseAll() {
        senseTemperature();
        senseAcceleration();
        sensePressure();
        senseHumidity();
    }

    private void senseTemperature() {
        try {
            temperatureValue = temperatureSensor.getTemperatureCelsius();
        } catch (I2CException e) {
            e.printStackTrace();
        }
    }

    private void senseAcceleration() {
        try {
            accelerationSensor.getValuesRaw(accelValue, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sensePressure() {
        try {
            pressureSensor.startBothConversion();
            Thread.sleep(MPL115A2.BOTH_CONVERSION_TIME);
            int pressurePr = pressureSensor.getPressureRaw();
            int tempRaw = pressureSensor.getTemperatureRaw();
            pressureValue = pressureSensor.compensate(pressurePr, tempRaw);
        } catch (Exception e) {
            System.out.println("MPL115A2 error");
        }
    }

    private void senseHumidity() {
        try {
            // humidity conversion
            Thread.sleep(1000);
            humiditySensor.startRelativeHumidityConversion();
            Thread.sleep(100);
            int rawRH = humiditySensor.getRelativeHumidityRaw();
            float rh = SHT21.convertRawRHToRHw(rawRH);
            humidityValue = rh;
        } catch (Exception e) {
            System.out.println("SHT21 error");
        }

    }
}
