package fr.imta.smartgrid.server.handlers;

import java.util.List;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class HandlerIngressWindTurbine implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerIngressWindTurbine(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response().setStatusCode(500).end(new JsonObject().put("error", "Invalid JSON payload").encode());
                return;
            }

            int turbineId = body.getInteger("windturbine");
            long timestamp = body.getLong("timestamp");
            JsonObject data = body.getJsonObject("data");
            
            if (data == null) {
                context.response().setStatusCode(500).end(new JsonObject().put("error", "Missing data field").encode());
                return;
            }

            double speed = data.getDouble("speed");
            double power = data.getDouble("power");

            WindTurbine turbine = db.find(WindTurbine.class, turbineId);
            if (turbine == null) {
                context.response().setStatusCode(404).end(new JsonObject().put("error", "Wind turbine not found").encode());
                return;
            }

            Measurement speedMeasurement = null;
            Measurement powerMeasurement = null;
            Measurement energyMeasurement = null;

            if (turbine.getMeasurements() != null) {
                for (Measurement m : turbine.getMeasurements()) {
                    if ("speed".equals(m.getName())) speedMeasurement = m;
                    else if ("power".equals(m.getName())) powerMeasurement = m;
                    else if ("total_energy_produced".equals(m.getName())) energyMeasurement = m;
                }
            }

            double lastEnergyValue = 0.0;
            if (energyMeasurement != null) {
                TypedQuery<DataPoint> query = db.createQuery(
                        "SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement ORDER BY dp.timestamp DESC", 
                        DataPoint.class);
                query.setParameter("measurement", energyMeasurement);
                query.setMaxResults(1);
                List<DataPoint> results = query.getResultList();
                if (!results.isEmpty()) {
                    lastEnergyValue = results.get(0).getValue();
                }
            }

            double newEnergyValue = lastEnergyValue + (power * (1.0 / 60.0));

            EntityTransaction tx = db.getTransaction();
            tx.begin();

            if (speedMeasurement != null) {
                DataPoint dp = new DataPoint();
                dp.setMeasurement(speedMeasurement);
                dp.setTimestamp(timestamp);
                dp.setValue(speed);
                db.persist(dp);
            }

            if (powerMeasurement != null) {
                DataPoint dp = new DataPoint();
                dp.setMeasurement(powerMeasurement);
                dp.setTimestamp(timestamp);
                dp.setValue(power);
                db.persist(dp);
            }

            if (energyMeasurement != null) {
                DataPoint dp = new DataPoint();
                dp.setMeasurement(energyMeasurement);
                dp.setTimestamp(timestamp);
                dp.setValue(newEnergyValue);
                db.persist(dp);
            }

            tx.commit();

            context.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("status", "success").encode());

        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            context.response().setStatusCode(500).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }
}
