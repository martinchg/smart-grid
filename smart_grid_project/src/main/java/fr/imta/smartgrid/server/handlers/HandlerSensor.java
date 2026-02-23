package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Consumer;
import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.Sensor;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerSensor implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerSensor(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String idParam = context.pathParam("id");
            int id = Integer.parseInt(idParam);

            Sensor sensor = db.find(Sensor.class, id);

            if (sensor == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Capteur non trouvé").encode());
                return;
            }

            JsonObject json = new JsonObject();
            json.put("id", sensor.getId());
            json.put("name", sensor.getName());
            json.put("description", sensor.getDescription());
            json.put("kind", getSensorKind(sensor));

            if (sensor.getGrid() != null) {
                json.put("grid", sensor.getGrid().getId());
            }

            JsonArray measurementsArray = new JsonArray();
            sensor.getMeasurements().forEach(m -> measurementsArray.add(m.getId()));
            json.put("available_measurements", measurementsArray);

            JsonArray ownersArray = new JsonArray();
            sensor.getOwners().forEach(o -> ownersArray.add(o.getId()));
            json.put("owners", ownersArray);

            addSpecificFields(sensor, json);

            context.response()
                .putHeader("content-type", "application/json")
                .end(json.encode());

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "ID de capteur invalide").encode());
        } catch (Exception e) {
            context.response()
                .setStatusCode(500)
                .end(new JsonObject().put("error", "Error: " + e.getMessage()).encode());
        }
    }

    private String getSensorKind(Sensor sensor) {
        if (sensor instanceof SolarPanel) {
            return "SolarPanel";
        } else if (sensor instanceof WindTurbine) {
            return "WindTurbine";
        } else if (sensor instanceof EVCharger) {
            return "EVCharger";
        } else if (sensor instanceof Producer) {
            return "Producer";
        } else if (sensor instanceof Consumer) {
            return "Consumer";
        } else {
            return "Sensor";
        }
    }

    private void addSpecificFields(Sensor sensor, JsonObject json) {
        if (sensor instanceof Producer) {
            Producer producer = (Producer) sensor;
            json.put("power_source", producer.getPowerSource());
            
            if (sensor instanceof SolarPanel) {
                json.put("efficiency", ((SolarPanel) sensor).getEfficiency());
            } else if (sensor instanceof WindTurbine) {
                WindTurbine wt = (WindTurbine) sensor;
                json.put("height", wt.getHeight());
                json.put("blade_length", wt.getBladeLength());
            }
        } else if (sensor instanceof Consumer) {
            Consumer consumer = (Consumer) sensor;
            json.put("max_power", consumer.getMaxPower());

            if (sensor instanceof EVCharger) {
                EVCharger ev = (EVCharger) sensor;
                json.put("voltage", ev.getVoltage());
                json.put("maxAmp", ev.getMaxAmp());
                json.put("type", ev.getType());
            }
        }
    }
}
