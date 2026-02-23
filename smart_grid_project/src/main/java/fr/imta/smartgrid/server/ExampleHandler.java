package fr.imta.smartgrid.server;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ExampleHandler implements Handler<RoutingContext> {

    EntityManager db;

    public ExampleHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        Query countQuery = db.createNativeQuery("SELECT count(*) FROM sensor");
        Long count = (Long) countQuery.getSingleResult();
        context.end("Count: " + count);

        Query sensorQuery = db.createNativeQuery("SELECT * FROM sensor WHERE id = ?", Sensor.class);
        sensorQuery.setParameter(1, 4);
        Sensor sensor = (Sensor) sensorQuery.getSingleResult();

        System.out.println("Sensor ID: " + sensor.getId());
        System.out.println("Sensor Name: " + sensor.getName());
        System.out.println("Sensor Description: " + sensor.getDescription());

        sensor.setDescription("you can change attributes");

        EVCharger evCharger = new EVCharger();
        evCharger.setName("my wonderful EVCharger");

        db.getTransaction().begin();
        db.persist(evCharger);
        db.persist(sensor);
        db.getTransaction().commit();
    }
}
