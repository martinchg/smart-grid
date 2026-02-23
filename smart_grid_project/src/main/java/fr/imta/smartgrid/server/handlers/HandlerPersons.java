package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Person;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class HandlerPersons implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerPersons(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            TypedQuery<Integer> query = db.createQuery("SELECT p.id FROM Person p", Integer.class);
            List<Integer> persons = query.getResultList();

            JsonArray jsonArray = new JsonArray();
            persons.forEach(jsonArray::add);

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
