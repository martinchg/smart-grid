package fr.imta.smartgrid.server.handlers;

import java.util.ArrayList;
import java.util.List;

import fr.imta.smartgrid.model.Grid;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class HandlerCreatePerson implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerCreatePerson(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("erreur", "Corps JSON invalide")
                                .encode());
                return;
            }

            if (!body.containsKey("first_name") || !body.containsKey("last_name") || !body.containsKey("grid")) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("erreur", "Champs requis manquants : first_name, last_name, grid")
                                .encode());
                return;
            }

            Integer gridId = body.getInteger("grid");
            Grid grid = db.find(Grid.class, gridId);

            if (grid == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject()
                                .put("erreur", "Grille introuvable")
                                .encode());
                return;
            }

            Person person = new Person();
            person.setFirstName(body.getString("first_name"));
            person.setLastName(body.getString("last_name"));
            person.setGrid(grid);

            if (body.containsKey("owned_sensors")) {
                JsonArray sensorsJson = body.getJsonArray("owned_sensors");
                List<Sensor> sensorsList = new ArrayList<>();
                for (int i = 0; i < sensorsJson.size(); i++) {
                    Integer sensorId = sensorsJson.getInteger(i);
                    Sensor sensor = db.find(Sensor.class, sensorId);
                    if (sensor != null) {
                        sensorsList.add(sensor);
                    }
                }
                person.setSensors(sensorsList);
            }

            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            db.persist(person);
            transaction.commit();

            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("id", person.getId())
                            .encode());

        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("erreur", e.getMessage())
                            .encode());
        }
    }
}
