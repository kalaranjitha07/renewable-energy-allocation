package com.kathir.energy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_records")
public class AllocationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int intervalNumber;
    private Long loadId;
    private String loadName;
    private String category;
    private double requestedKwh;
    private double allocatedKwh;
    private double unmetKwh;
    private double curtailedKwh;
    private double generationKwh;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AllocationRecord() {}
    public AllocationRecord(int intervalNumber, Long loadId, String loadName, String category,
            double requestedKwh, double allocatedKwh, double unmetKwh, double curtailedKwh, double generationKwh) {
        this.intervalNumber=intervalNumber; this.loadId=loadId; this.loadName=loadName; this.category=category;
        this.requestedKwh=requestedKwh; this.allocatedKwh=allocatedKwh; this.unmetKwh=unmetKwh;
        this.curtailedKwh=curtailedKwh; this.generationKwh=generationKwh;
    }
    public Long getId(){return id;}
    public int getIntervalNumber(){return intervalNumber;}
    public Long getLoadId(){return loadId;}
    public String getLoadName(){return loadName;}
    public String getCategory(){return category;}
    public double getRequestedKwh(){return requestedKwh;}
    public double getAllocatedKwh(){return allocatedKwh;}
    public double getUnmetKwh(){return unmetKwh;}
    public double getCurtailedKwh(){return curtailedKwh;}
    public double getGenerationKwh(){return generationKwh;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
