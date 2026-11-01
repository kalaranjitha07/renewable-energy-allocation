package com.kathir.energy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "consumer_loads")
public class ConsumerLoad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private double demandKwh;
    private double minimumServiceKwh;
    private boolean divisible;
    private int priority;
    private int requestOrder;

    public ConsumerLoad() {}
    public ConsumerLoad(String name, String category, double demandKwh, double minimumServiceKwh,
                        boolean divisible, int priority, int requestOrder) {
        this.name=name; this.category=category; this.demandKwh=demandKwh;
        this.minimumServiceKwh=minimumServiceKwh; this.divisible=divisible;
        this.priority=priority; this.requestOrder=requestOrder;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name=name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category=category; }
    public double getDemandKwh() { return demandKwh; }
    public void setDemandKwh(double demandKwh) { this.demandKwh=demandKwh; }
    public double getMinimumServiceKwh() { return minimumServiceKwh; }
    public void setMinimumServiceKwh(double minimumServiceKwh) { this.minimumServiceKwh=minimumServiceKwh; }
    public boolean isDivisible() { return divisible; }
    public void setDivisible(boolean divisible) { this.divisible=divisible; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority=priority; }
    public int getRequestOrder() { return requestOrder; }
    public void setRequestOrder(int requestOrder) { this.requestOrder=requestOrder; }
}
