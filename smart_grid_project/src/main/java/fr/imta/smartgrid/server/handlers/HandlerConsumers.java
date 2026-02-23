package fr.imta.smartgrid.server.handlers;

import java.util.List;

import fr.imta.smartgrid.model.Consumer;
import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class HandlerConsumers implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerConsumers(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String method = context.request().method().name();
        String idParam = context.pathParam("id");

        try {
            if ("GET".equals(method)) {
                if (idParam != null) {
                    getConsumer(context, Long.parseLong(idParam));
                } else {
                    getAllConsumers(context);
                }
            } else if ("POST".equals(method)) {
                createConsumer(context);
            } else if ("PUT".equals(method) && idParam != null) {
                updateConsumer(context, Long.parseLong(idParam));
            } else if ("DELETE".equals(method) && idParam != null) {
                deleteConsumer(context, Long.parseLong(idParam));
            } else {
                context.response().setStatusCode(400).end("Bad request");
            }
        } catch (NumberFormatException e) {
            context.response().setStatusCode(400).end("Invalid ID format");
        } catch (Exception e) {
            context.response().setStatusCode(500).end("Error: " + e.getMessage());
        }
    }

    private void getAllConsumers(RoutingContext context) {
        TypedQuery<Consumer> query = db.createQuery("SELECT c FROM Consumer c", Consumer.class);
        List<Consumer> consumers = query.getResultList();
        JsonArray result = new JsonArray();

        for (Consumer c : consumers) {
            JsonObject json = toConsumerJson(c);
            result.add(json);
        }

        context.response()
                .putHeader("content-type", "application/json")
                .end(result.encode());
    }

    private void getConsumer(RoutingContext context, long id) {
        Consumer c = db.find(Consumer.class, (int) id); // Casting to int as ID is int in model
        if (c == null) {
            context.response().setStatusCode(404).end("Consumer not found");
            return;
        }

        context.response()
                .putHeader("content-type", "application/json")
                .end(toConsumerJson(c).encode());
    }

    private JsonObject toConsumerJson(Consumer c) {
        JsonObject json = new JsonObject()
                .put("id", c.getId())
                .put("name", c.getName())
                .put("description", c.getDescription())
                .put("max_power", c.getMaxPower());

        if (c.getGrid() != null) {
            json.put("grid", c.getGrid().getId());
        }

        String kind = (c instanceof EVCharger) ? "EVCharger" : "Consumer";
        json.put("kind", kind);

        JsonArray measurements = new JsonArray();
        if (c.getMeasurements() != null) {
            c.getMeasurements().forEach(m -> measurements.add(m.getId()));
        }
        json.put("available_measurements", measurements);

        JsonArray owners = new JsonArray();
        if (c.getOwners() != null) {
            c.getOwners().forEach(p -> owners.add(p.getId()));
        }
        json.put("owners", owners);

        if (c instanceof EVCharger) {
            EVCharger ev = (EVCharger) c;
            json.put("voltage", ev.getVoltage());
            json.put("maxAmp", ev.getMaxAmp());
            json.put("type", ev.getType());
        }

        return json;
    }

    private void createConsumer(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            String kind = body.getString("kind", "Consumer");
            Consumer consumer;

            if ("EVCharger".equals(kind)) {
                EVCharger ev = new EVCharger();
                ev.setVoltage(body.getDouble("voltage", 0.0).intValue());
                ev.setMaxAmp(body.getDouble("maxAmp", 0.0).intValue());
                ev.setType(body.getString("type", ""));
                consumer = ev;
            } else {
                // Anonymous subclass for abstract Consumer
                consumer = new Consumer() {}; 
            }

            updateConsumerFromJson(consumer, body);

            EntityTransaction tx = db.getTransaction();
            tx.begin();
            db.persist(consumer);
            tx.commit();

            context.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(toConsumerJson(consumer).encode());

        } catch (Exception e) {
            context.response().setStatusCode(400).end("Error creating consumer: " + e.getMessage());
        }
    }

    private void updateConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, (int) id);
            if (consumer == null) {
                context.response().setStatusCode(404).end("Consumer not found");
                return;
            }

            JsonObject body = context.getBodyAsJson();
            EntityTransaction tx = db.getTransaction();
            tx.begin();
            
            updateConsumerFromJson(consumer, body);
            
            if (consumer instanceof EVCharger && "EVCharger".equals(body.getString("kind"))) {
                 EVCharger ev = (EVCharger) consumer;
                 if (body.containsKey("voltage")) ev.setVoltage(body.getDouble("voltage").intValue());
                 if (body.containsKey("maxAmp")) ev.setMaxAmp(body.getDouble("maxAmp").intValue());
                 if (body.containsKey("type")) ev.setType(body.getString("type"));
            }

            tx.commit();

            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(toConsumerJson(consumer).encode());

        } catch (Exception e) {
            context.response().setStatusCode(400).end("Error updating: " + e.getMessage());
        }
    }

    private void updateConsumerFromJson(Consumer consumer, JsonObject body) {
        if (body.containsKey("name")) consumer.setName(body.getString("name"));
        if (body.containsKey("description")) consumer.setDescription(body.getString("description"));
        if (body.containsKey("max_power")) consumer.setMaxPower(body.getDouble("max_power"));
        
        if (body.containsKey("grid")) {
            Long gridId = body.getLong("grid");
            Grid grid = db.find(Grid.class, gridId.intValue());
            if (grid != null) {
                consumer.setGrid(grid);
            }
        }
    }

    private void deleteConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, (int) id);
            if (consumer == null) {
                context.response().setStatusCode(404).end("Consumer not found");
                return;
            }

            EntityTransaction tx = db.getTransaction();
            tx.begin();
            db.remove(consumer);
            tx.commit();

            context.response().setStatusCode(204).end();

        } catch (Exception e) {
            context.response().setStatusCode(500).end("Error deleting: " + e.getMessage());
        }
    }
}
