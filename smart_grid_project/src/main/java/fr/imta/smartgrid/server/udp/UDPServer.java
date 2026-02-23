package fr.imta.smartgrid.server.udp;

import java.util.Optional;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import fr.imta.smartgrid.model.SolarPanel;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import jakarta.persistence.EntityManager;

public class UDPServer {

    private final Vertx vertx;
    private final EntityManager db;
    private DatagramSocket socket;

    public UDPServer(Vertx vertx, EntityManager db) {
        this.vertx = vertx;
        this.db = db;
    }

    public void start(int port) {
        DatagramSocketOptions options = new DatagramSocketOptions();
        this.socket = vertx.createDatagramSocket(options);
        
        this.socket.listen(port, "0.0.0.0", asyncResult -> {
            if (asyncResult.succeeded()) {
                System.out.println("UDP Server listening on port " + port);
                this.socket.handler(packet -> {
                    String data = packet.data().toString();
                    processUDPMessage(data);
                });
            } else {
                System.err.println("Listen failed: " + asyncResult.cause().getMessage());
            }
        });
    }

    private void processUDPMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length != 4) {
            System.err.println("Invalid message format: " + message);
            return;
        }

        try {
            int solarPanelId = Integer.parseInt(parts[0]);
            double currentPower = Double.parseDouble(parts[1]); // produced power in kW
            double temperature = Double.parseDouble(parts[2]);
            long timestamp = Long.parseLong(parts[3]);

            SolarPanel solarPanel = db.find(SolarPanel.class, solarPanelId);
            if (solarPanel == null) {
                System.err.println("SolarPanel not found: " + solarPanelId);
                return;
            }

            Measurement temperatureMeasurement = null;
            Measurement powerMeasurement = null;
            Measurement totalEnergyMeasurement = null;

            for (Measurement m : solarPanel.getMeasurements()) {
                if ("temperature".equals(m.getName())) {
                    temperatureMeasurement = m;
                } else if ("power".equals(m.getName())) {
                    powerMeasurement = m;
                } else if ("total_energy_produced".equals(m.getName())) {
                    totalEnergyMeasurement = m;
                }
            }

            // Calculate energy produced since last measurement (approx)
            // Assuming 1 second interval basically, or just integration.
            // But logic in decompiled code:
            // var14 = 0.0;
            // Get last datapoint for total_energy_produced, get its value.
            // var16 = var14 + currentPower * (1.0/60.0); // Wait, decompiled said 0.016666666666666666d which is 1/60.
            // So it seems it assumes the power is in kW and the interval is 1 minute? Or maybe power is Watts and interval is ...?
            // If power is in kW. 1 minute = 1/60 hour. So kWh produced = power * 1/60.
            
            double lastTotalEnergy = 0.0;
            if (totalEnergyMeasurement != null) {
                Optional<DataPoint> lastDataPoint = db.createQuery("SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement ORDER BY dp.timestamp DESC", DataPoint.class)
                    .setParameter("measurement", totalEnergyMeasurement)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst();
                
                if (lastDataPoint.isPresent()) {
                    lastTotalEnergy = lastDataPoint.get().getValue();
                }
            }

            // Accumulate energy. 1/60 factor implies we receive data every minute if power is instantaneous power?
            // Or maybe it's just a set factor.
            double newTotalEnergy = lastTotalEnergy + (currentPower * (1.0 / 60.0));

            if (temperatureMeasurement != null) {
                saveDataPoint(temperatureMeasurement, timestamp, temperature);
            }

            if (powerMeasurement != null) {
                saveDataPoint(powerMeasurement, timestamp, currentPower);
            }

            if (totalEnergyMeasurement != null) {
                saveDataPoint(totalEnergyMeasurement, timestamp, newTotalEnergy);
            }

            System.out.println("Processed UDP: Panel=" + solarPanelId + ", Power=" + currentPower + ", Temp=" + temperature + ", Time=" + timestamp);

        } catch (Exception e) {
            System.err.println("Error processing UDP message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDataPoint(Measurement measurement, long timestamp, double value) {
        try {
            DataPoint dataPoint = new DataPoint();
            dataPoint.setMeasurement(measurement);
            dataPoint.setTimestamp(timestamp);
            dataPoint.setValue(value);

            db.getTransaction().begin();
            db.persist(dataPoint);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            System.err.println("Error saving DataPoint: " + e.getMessage());
        }
    }
}
