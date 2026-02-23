package fr.imta.smartgrid.model;

import jakarta.persistence.Entity;

@Entity
public class WindTurbine extends Producer {
    
    private Double height;
    private Double bladeLength;

    public WindTurbine() {}

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getBladeLength() {
        return bladeLength;
    }

    public void setBladeLength(Double bladeLength) {
        this.bladeLength = bladeLength;
    }
}
