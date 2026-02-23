package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class HandlerProductionGrid implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerProductionGrid(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String idParam = context.pathParam("id");
            int id = Integer.parseInt(idParam);

            Grid grid = db.find(Grid.class, id);

            if (grid == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Grid not found").encode());
                return;
            }

            // The native query logic is reconstructed based on the meaning of the bytecode strings
            // It calculates total energy produced by summing up the latest 'total_energy_produced' measurement for all producers in the grid.
            String sql = "SELECT COALESCE(SUM(d.value), 0) FROM datapoint d JOIN measurement m ON d.measurement = m.id JOIN sensor s ON m.sensor = s.id JOIN producer p ON p.id = s.id WHERE s.grid = ?1 AND m.name = 'total_energy_produced' AND d.timestamp = (SELECT MAX(d2.timestamp) FROM datapoint d2 WHERE d2.measurement = d.measurement)";
            
            Query query = db.createNativeQuery(sql);
            query.setParameter(1, id);
            Double totalProduction = (Double) query.getSingleResult();

            context.response()
                .putHeader("content-type", "application/json")
                .end(String.valueOf(totalProduction));

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "Invalid grid ID").encode());
        } catch (Exception e) {
            context.response()
                .setStatusCode(500)
                .end(new JsonObject().put("error", "Error: " + e.getMessage()).encode());
        }
    }
}
