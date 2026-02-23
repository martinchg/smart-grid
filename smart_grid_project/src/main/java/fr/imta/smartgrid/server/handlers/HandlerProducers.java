package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class HandlerProducers implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerProducers(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            TypedQuery<Producer> query = db.createQuery("SELECT p FROM Producer p", Producer.class);
            List<Producer> producers = query.getResultList();

            JsonArray jsonArray = new JsonArray();

            for (Producer producer : producers) {
                JsonObject json = new JsonObject();
                json.put("id", producer.getId());
                json.put("name", producer.getName());
                json.put("description", producer.getDescription());
                json.put("power_source", producer.getPowerSource());

                if (producer.getGrid() != null) {
                    json.put("grid", producer.getGrid().getId());
                }

                String kind = "Producer";
                if (producer instanceof SolarPanel) {
                    kind = "SolarPanel";
                } else if (producer instanceof WindTurbine) {
                    kind = "WindTurbine";
                }
                json.put("kind", kind);

                JsonArray measurementsArray = new JsonArray();
                producer.getMeasurements().forEach(m -> measurementsArray.add(m.getId()));
                json.put("available_measurements", measurementsArray);

                JsonArray ownersArray = new JsonArray();
                producer.getOwners().forEach(o -> ownersArray.add(o.getId()));
                json.put("owners", ownersArray);

                if (producer instanceof SolarPanel) {
                    json.put("efficiency", ((SolarPanel) producer).getEfficiency());
                } else if (producer instanceof WindTurbine) {
                    WindTurbine wt = (WindTurbine) producer;
                    json.put("height", wt.getHeight());
                    json.put("blade_length", wt.getBladeLength());
                }

                jsonArray.add(json);
            }

            context.response()
                .putHeader("content-type", "application/json")
                .end(jsonArray.encode());

        } catch (Exception e) {
            context.response()
                .setStatusCode(500)
                .end("Error: " + e.getMessage());
        }
    }
}
