package fr.imta.smartgrid.model;

import jakarta.persistence.Entity;

@Entity
public abstract class Producer extends Sensor {
    
    private String powerSource;

    public Producer() {}

    public String getPowerSource() {
        return powerSource;
    }

    public void setPowerSource(String powerSource) {
        this.powerSource = powerSource;
    }
}
