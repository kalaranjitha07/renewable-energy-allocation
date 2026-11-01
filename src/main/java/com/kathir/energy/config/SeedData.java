package com.kathir.energy.config;

import com.kathir.energy.model.*;
import com.kathir.energy.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seed(EnergySourceRepository sources, ConsumerLoadRepository loads) {
        return args -> {
            if (sources.count()==0) {
                sources.save(new EnergySource("Solar Array A","Solar",18));
                sources.save(new EnergySource("Solar Array B","Solar",12));
                sources.save(new EnergySource("Village Wind Unit","Wind",20));
            }
            if (loads.count()==0) {
                loads.save(new ConsumerLoad("Community Clinic","Critical",8,5,false,1,1));
                loads.save(new ConsumerLoad("Emergency Lighting","Critical",4,2,false,1,2));
                loads.save(new ConsumerLoad("Water Pump 1","Essential",6,2,true,2,3));
                loads.save(new ConsumerLoad("Water Pump 2","Essential",5,2,true,2,4));
                loads.save(new ConsumerLoad("Village Network","Essential",3,1,true,2,5));
                loads.save(new ConsumerLoad("Household Cluster A","Deferrable",8,0,true,3,6));
                loads.save(new ConsumerLoad("Household Cluster B","Deferrable",7,0,true,3,7));
                loads.save(new ConsumerLoad("Household Cluster C","Deferrable",6,0,true,3,8));
                loads.save(new ConsumerLoad("Workshop","Deferrable",5,0,true,3,9));
                loads.save(new ConsumerLoad("Street Decorative Lights","Deferrable",2,0,true,3,10));
            }
        };
    }
}
