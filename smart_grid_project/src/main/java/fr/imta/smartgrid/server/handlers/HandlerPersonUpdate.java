package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.ArrayList;
import java.util.List;

public class HandlerPersonUpdate implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerPersonUpdate(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String idParam = context.pathParam("id");
            int id = Integer.parseInt(idParam);
            JsonObject body = context.getBodyAsJson();

            if (body == null) {
                context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Corps JSON invalide").encode());
                return;
            }

            Person person = db.find(Person.class, id);
            if (person == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Personne non trouvée").encode());
                return;
            }

            EntityTransaction transaction = db.getTransaction();
            transaction.begin();

            if (body.containsKey("first_name")) {
                person.setFirstName(body.getString("first_name"));
            }

            if (body.containsKey("last_name")) {
                person.setLastName(body.getString("last_name"));
            }

            if (body.containsKey("grid")) {
                Integer gridId = body.getInteger("grid");
                if (gridId != null) {
                    Grid grid = db.find(Grid.class, gridId);
                    if (grid != null) {
                        person.setGrid(grid);
                    }
                }
            }

            if (body.containsKey("owned_sensors")) {
                JsonArray sensorsArray = body.getJsonArray("owned_sensors");
                List<Sensor> newSensors = new ArrayList<>();
                for (int i = 0; i < sensorsArray.size(); i++) {
                    Integer sensorId = sensorsArray.getInteger(i);
                    Sensor sensor = db.find(Sensor.class, sensorId);
                    if (sensor != null) {
                        newSensors.add(sensor);
                    }
                }
                person.setSensors(newSensors);
            }

            db.persist(person);
            transaction.commit();

            context.response()
                .setStatusCode(200)
                .end(new JsonObject().put("status", "succès").encode());

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "ID de personne invalide").encode());
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            context.response()
                .setStatusCode(500)
                .end(new JsonObject().put("error", "Error: " + e.getMessage()).encode());
        }
    }
}
