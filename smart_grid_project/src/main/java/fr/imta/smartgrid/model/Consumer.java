package fr.imta.smartgrid.model;

import jakarta.persistence.Entity;

@Entity
public abstract class Consumer extends Sensor {
    
    private Double maxPower;

    public Consumer() {}

    public Double getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(Double maxPower) {
        this.maxPower = maxPower;
    }
}
