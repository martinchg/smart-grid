package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Person;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class HandlerDeletePerson implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerDeletePerson(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String idParam = context.pathParam("id");
        try {
            int id = Integer.parseInt(idParam);
            Person person = db.find(Person.class, id);

            if (person == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject()
                                .put("error", "Person not found")
                                .encode());
                return;
            }

            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            db.remove(person);
            transaction.commit();

            context.response()
                    .setStatusCode(204)
                    .end();

        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject()
                            .put("error", "Invalid person ID")
                            .encode());
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("error", "Error deleting person: " + e.getMessage())
                            .encode());
        }
    }
}
