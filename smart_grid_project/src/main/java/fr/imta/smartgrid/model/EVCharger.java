package fr.imta.smartgrid.model;

import jakarta.persistence.Entity;

@Entity
public class EVCharger extends Consumer {
    
    private int voltage;
    private int maxAmp;
    private String type;

    public EVCharger() {}

    public int getVoltage() {
        return voltage;
    }

    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }

    public int getMaxAmp() {
        return maxAmp;
    }

    public void setMaxAmp(int maxAmp) {
        this.maxAmp = maxAmp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
