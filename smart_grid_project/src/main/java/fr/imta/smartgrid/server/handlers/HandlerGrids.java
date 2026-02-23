package fr.imta.smartgrid.server.handlers;

import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class HandlerGrids implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerGrids(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            TypedQuery<Integer> query = db.createQuery("SELECT g.id FROM Grid g", Integer.class);
            List<Integer> gridIds = query.getResultList();
            JsonArray result = new JsonArray();
            gridIds.forEach(result::add);

            context.response()
                    .putHeader("content-type", "application/json")
                    .end(result.encode());

        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Error: " + e.getMessage());
        }
    }
}
