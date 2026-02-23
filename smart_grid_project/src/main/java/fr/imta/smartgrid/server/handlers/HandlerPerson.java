package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Person;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerPerson implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerPerson(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String idParam = context.pathParam("id");
            int id = Integer.parseInt(idParam);

            Person person = db.find(Person.class, id);

            if (person == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Personne non trouvée").encode());
                return;
            }

            JsonObject json = new JsonObject();
            json.put("id", person.getId());
            json.put("first_name", person.getFirstName());
            json.put("last_name", person.getLastName());

            if (person.getGrid() != null) {
                json.put("grid", person.getGrid().getId());
            }

            JsonArray sensorsArray = new JsonArray();
            person.getSensors().forEach(s -> sensorsArray.add(s.getId()));
            json.put("owned_sensors", sensorsArray);

            context.response()
                .putHeader("content-type", "application/json")
                .end(json.encode());

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "ID de personne invalide").encode());
        } catch (Exception e) {
            context.response()
                .setStatusCode(500)
                .end(new JsonObject().put("error", "Error: " + e.getMessage()).encode());
        }
    }
}
