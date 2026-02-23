package fr.imta.smartgrid.server;

import java.util.HashMap;
import java.util.Map;

import fr.imta.smartgrid.server.handlers.HandlerConsumers;
import fr.imta.smartgrid.server.handlers.HandlerConsumptionGrid;
import fr.imta.smartgrid.server.handlers.HandlerCreatePerson;
import fr.imta.smartgrid.server.handlers.HandlerDeletePerson;
import fr.imta.smartgrid.server.handlers.HandlerGrid;
import fr.imta.smartgrid.server.handlers.HandlerGrids;
import fr.imta.smartgrid.server.handlers.HandlerIngressWindTurbine;
import fr.imta.smartgrid.server.handlers.HandlerMeasurement;
import fr.imta.smartgrid.server.handlers.HandlerPerson;
import fr.imta.smartgrid.server.handlers.HandlerPersonUpdate;
import fr.imta.smartgrid.server.handlers.HandlerPersons;
import fr.imta.smartgrid.server.handlers.HandlerProducers;
import fr.imta.smartgrid.server.handlers.HandlerProductionGrid;
import fr.imta.smartgrid.server.handlers.HandlerSensor;
import fr.imta.smartgrid.server.handlers.HandlerSensorsKind;
import fr.imta.smartgrid.server.handlers.HandlerUpdateSensor;
import fr.imta.smartgrid.server.handlers.HandlerValuesMeasurement;
import fr.imta.smartgrid.server.udp.UDPServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

public class VertxServer {

    private Vertx vertx;
    private EntityManager db;
    private UDPServer udpServer;

    public VertxServer() {
        this.vertx = Vertx.vertx();
        
        Map<String, String> properties = new HashMap<>();
        properties.put("eclipselink.logging.level", "FINE");
        properties.put("min", "1");
        properties.put("eclipselink.target-server", "None");

        this.db = Persistence.createEntityManagerFactory("smart-grid", properties).createEntityManager();
        this.udpServer = new UDPServer(this.vertx, this.db);
    }

    public void start() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/hello").handler(new ExampleHandler(db));
        
        router.get("/grids").handler(new HandlerGrids(db));
        router.get("/grid/:id").handler(new HandlerGrid(db));
        router.get("/grid/:id/production").handler(new HandlerProductionGrid(db));
        router.get("/grid/:id/consumption").handler(new HandlerConsumptionGrid(db));
        
        router.get("/persons").handler(new HandlerPersons(db));
        router.get("/person/:id").handler(new HandlerPerson(db));
        router.post("/person/:id").handler(new HandlerPersonUpdate(db));
        router.delete("/person/:id").handler(new HandlerDeletePerson(db));
        router.put("/person").handler(new HandlerCreatePerson(db));
        
        router.get("/sensor/:id").handler(new HandlerSensor(db));
        router.get("/sensors/:kind").handler(new HandlerSensorsKind(db));
        
        router.get("/consumers").handler(new HandlerConsumers(db));
        router.get("/producers").handler(new HandlerProducers(db));
        
        router.post("/sensor/:id").handler(new HandlerUpdateSensor(db));
        
        router.get("/measurement/:id").handler(new HandlerMeasurement(db));
        router.get("/measurement/:id/values").handler(new HandlerValuesMeasurement(db));
        
        router.post("/ingress/windturbine").handler(new HandlerIngressWindTurbine(db));

        udpServer.start(12345);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080);
            
        System.out.println("Server started on port 8080");
    }

    public static void main(String[] args) {
        new VertxServer().start();
    }
}
