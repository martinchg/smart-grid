package fr.imta.smartgrid.model;

import jakarta.persistence.Entity;

@Entity
public class SolarPanel extends Producer {
    
    private float efficiency;

    public SolarPanel() {}

    public float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(float efficiency) {
        this.efficiency = efficiency;
    }
}
