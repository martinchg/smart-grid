package fr.imta.smartgrid.server.handlers;

import java.util.ArrayList;
import java.util.List;

import fr.imta.smartgrid.model.Consumer;
import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.Sensor;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class HandlerUpdateSensor implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerUpdateSensor(EntityManager db) {
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

            Sensor sensor = db.find(Sensor.class, id);
            if (sensor == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Capteur non trouvé").encode());
                return;
            }

            EntityTransaction transaction = db.getTransaction();
            transaction.begin();

            if (body.containsKey("name")) {
                sensor.setName(body.getString("name"));
            }

            if (body.containsKey("description")) {
                sensor.setDescription(body.getString("description"));
            }

            if (body.containsKey("owners")) {
                JsonArray ownersArray = body.getJsonArray("owners");
                List<Person> newOwners = new ArrayList<>();
                for (int i = 0; i < ownersArray.size(); i++) {
                    Integer ownerId = ownersArray.getInteger(i);
                    Person person = db.find(Person.class, ownerId);
                    if (person != null) {
                        newOwners.add(person);
                    }
                }
                sensor.setOwners(newOwners);
            }

            if (sensor instanceof Producer) {
                Producer producer = (Producer) sensor;
                if (body.containsKey("power_source")) {
                    producer.setPowerSource(body.getString("power_source"));
                }
                if (producer instanceof SolarPanel && body.containsKey("efficiency")) {
                    ((SolarPanel) producer).setEfficiency(body.getDouble("efficiency").floatValue());
                } else if (producer instanceof WindTurbine) {
                    WindTurbine wt = (WindTurbine) producer;
                    if (body.containsKey("height")) {
                        wt.setHeight(body.getDouble("height"));
                    }
                    if (body.containsKey("blade_length")) {
                        wt.setBladeLength(body.getDouble("blade_length"));
                    }
                }
            } else if (sensor instanceof Consumer) {
                Consumer consumer = (Consumer) sensor;
                if (body.containsKey("max_power")) {
                    consumer.setMaxPower(body.getDouble("max_power"));
                }
                if (consumer instanceof EVCharger) {
                    EVCharger ev = (EVCharger) consumer;
                    if (body.containsKey("type")) {
                        ev.setType(body.getString("type"));
                    }
                    if (body.containsKey("voltage")) {
                        ev.setVoltage(body.getInteger("voltage"));
                    }
                    if (body.containsKey("maxAmp")) {
                        ev.setMaxAmp(body.getInteger("maxAmp"));
                    }
                }
            }

            db.persist(sensor);
            transaction.commit();

            context.response()
                .setStatusCode(200)
                .end(new JsonObject().put("status", "succès").encode());

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "ID de capteur invalide").encode());
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
