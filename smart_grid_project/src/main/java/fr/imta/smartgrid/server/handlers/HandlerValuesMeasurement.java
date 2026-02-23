package fr.imta.smartgrid.server.handlers;

import java.util.List;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class HandlerValuesMeasurement implements Handler<RoutingContext> {

    private final EntityManager db;

    public HandlerValuesMeasurement(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            String idParam = context.pathParam("id");
            int id = Integer.parseInt(idParam);

            Measurement measurement = db.find(Measurement.class, id);

            if (measurement == null) {
                context.response()
                    .setStatusCode(404)
                    .end(new JsonObject().put("error", "Measurement not found").encode());
                return;
            }

            String fromParam = context.request().getParam("from");
            String toParam = context.request().getParam("to");
            
            long fromTime = (fromParam != null) ? Long.parseLong(fromParam) : 0L;
            long toTime = (toParam != null) ? Long.parseLong(toParam) : Long.MAX_VALUE;

            TypedQuery<DataPoint> query = db.createQuery(
                "SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement AND dp.timestamp >= :fromTime AND dp.timestamp <= :toTime ORDER BY dp.timestamp", 
                DataPoint.class
            );
            query.setParameter("measurement", measurement);
            query.setParameter("fromTime", fromTime);
            query.setParameter("toTime", toTime);
            
            List<DataPoint> dataPoints = query.getResultList();

            JsonObject json = new JsonObject();
            json.put("sensor_id", measurement.getSensor().getId());
            json.put("measurement_id", measurement.getId());

            JsonArray valuesArray = new JsonArray();
            for (DataPoint dp : dataPoints) {
                JsonObject dpJson = new JsonObject();
                dpJson.put("timestamp", dp.getTimestamp());
                dpJson.put("value", dp.getValue());
                valuesArray.add(dpJson);
            }
            json.put("values", valuesArray);

            context.response()
                .putHeader("content-type", "application/json")
                .end(json.encode());

        } catch (NumberFormatException e) {
            context.response()
                .setStatusCode(400)
                .end(new JsonObject().put("error", "Invalid parameter").encode());
        } catch (Exception e) {
            context.response()
                .setStatusCode(500)
                .end(new JsonObject().put("error", "Error: " + e.getMessage()).encode());
        }
    }
}
