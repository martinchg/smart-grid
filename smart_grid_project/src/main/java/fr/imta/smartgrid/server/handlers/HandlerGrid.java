package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerGrid implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerGrid(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String idParam = context.pathParam("id");
        try {
            int id = Integer.parseInt(idParam);
            Grid grid = db.find(Grid.class, id);

            if (grid == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject()
                                .put("error", "Grid not found")
                                .encode());
                return;
            }

            JsonArray users = new JsonArray();
            if (grid.getPersons() != null) {
                grid.getPersons().forEach(p -> users.add(p.getId()));
            }

            JsonArray sensors = new JsonArray();
            if (grid.getSensors() != null) {
                grid.getSensors().forEach(s -> sensors.add(s.getId()));
            }

            JsonObject response = new JsonObject()
                    .put("id", grid.getId())
                    .put("name", grid.getName())
                    .put("description", grid.getDescription())
                    .put("users", users)
                    .put("sensors", sensors);

            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());

        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject()
                            .put("error", "Invalid grid ID")
                            .encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("error", e.getMessage())
                            .encode());
        }
    }
}
