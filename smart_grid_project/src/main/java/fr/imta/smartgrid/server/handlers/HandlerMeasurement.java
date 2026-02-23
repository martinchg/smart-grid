package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Measurement;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerMeasurement implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerMeasurement(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String idParam = context.pathParam("id");
        try {
            int id = Integer.parseInt(idParam);
            Measurement measurement = db.find(Measurement.class, id);

            if (measurement == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject()
                                .put("erreur", "Mesure non trouvée")
                                .encode());
                return;
            }

            JsonObject response = new JsonObject()
                    .put("id", measurement.getId())
                    .put("nom", measurement.getName())
                    .put("unite", measurement.getUnit());

            if (measurement.getSensor() != null) {
                response.put("capteur", measurement.getSensor().getId());
            }

            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());

        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject()
                            .put("erreur", "Identifiant de mesure invalide")
                            .encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("erreur", e.getMessage())
                            .encode());
        }
    }
}
