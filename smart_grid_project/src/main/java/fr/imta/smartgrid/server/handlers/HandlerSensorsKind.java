package fr.imta.smartgrid.server.handlers;

import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class HandlerSensorsKind implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerSensorsKind(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String kind = context.pathParam("kind");
            List<Integer> ids;

            if ("SolarPanel".equals(kind)) {
                TypedQuery<Integer> query = db.createQuery("SELECT s.id FROM SolarPanel s", Integer.class);
                ids = query.getResultList();
            } else if ("WindTurbine".equals(kind)) {
                TypedQuery<Integer> query = db.createQuery("SELECT w.id FROM WindTurbine w", Integer.class);
                ids = query.getResultList();
            } else if ("EVCharger".equals(kind)) {
                TypedQuery<Integer> query = db.createQuery("SELECT e.id FROM EVCharger e", Integer.class);
                ids = query.getResultList();
            } else {
                ids = List.of();
            }

            JsonArray jsonArray = new JsonArray();
            ids.forEach(jsonArray::add);

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
