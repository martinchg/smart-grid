package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class HandlerConsumptionGrid implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerConsumptionGrid(EntityManager db) {
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

            String sql = "SELECT COALESCE(SUM(d.value), 0) FROM datapoint d " +
                         "JOIN measurement m ON d.measurement = m.id " +
                         "JOIN sensor s ON m.sensor = s.id " +
                         "JOIN consumer c ON c.id = s.id " +
                         "WHERE s.grid = ?1 AND m.name = 'total_energy_consumed' " +
                         "AND d.timestamp = (SELECT MAX(d2.timestamp) FROM datapoint d2 WHERE d2.measurement = d.measurement)";

            Query query = db.createNativeQuery(sql);
            query.setParameter(1, id);
            Double result = (Double) query.getSingleResult();

            context.response()
                    .putHeader("content-type", "application/json")
                    .end(String.valueOf(result));

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
