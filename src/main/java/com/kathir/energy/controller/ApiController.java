package com.kathir.energy.controller;

import com.kathir.energy.model.*;
import com.kathir.energy.repository.*;
import com.kathir.energy.service.AllocationService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    private final EnergySourceRepository sources;
    private final ConsumerLoadRepository loads;
    private final AllocationRecordRepository records;
    private final AllocationService service;

    public ApiController(EnergySourceRepository sources, ConsumerLoadRepository loads,
                         AllocationRecordRepository records, AllocationService service) {
        this.sources=sources; this.loads=loads; this.records=records; this.service=service;
    }
    @GetMapping("/dashboard") public Map<String,Object> dashboard(){ return service.dashboard(); }
    @GetMapping("/sources") public List<EnergySource> getSources(){ return sources.findAll(); }
    @PostMapping("/sources") public ResponseEntity<?> addSource(@RequestBody EnergySource source) {
        if (source.getName()==null || source.getName().isBlank() || source.getCapacityKwh()<0)
            return ResponseEntity.badRequest().body(Map.of("error","Provide a source name and non-negative capacity."));
        source.setName(source.getName().trim()); source.setId(null);
        if (source.getType()==null || source.getType().isBlank()) source.setType("Unspecified");
        return ResponseEntity.ok(sources.save(source));
    }
    @PutMapping("/sources/{id}") public ResponseEntity<?> updateSource(@PathVariable Long id, @RequestBody EnergySource source) {
        if (source.getName()==null || source.getName().isBlank() || source.getCapacityKwh()<0)
            return ResponseEntity.badRequest().body(Map.of("error","Provide a source name and non-negative capacity."));
        return sources.findById(id).map(existing -> {
            existing.setName(source.getName().trim()); existing.setCapacityKwh(source.getCapacityKwh());
            return ResponseEntity.ok(sources.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @DeleteMapping("/sources/{id}") public ResponseEntity<?> deleteSource(@PathVariable Long id) {
        if (!sources.existsById(id)) return ResponseEntity.notFound().build();
        sources.deleteById(id); return ResponseEntity.ok(Map.of("message","Renewable source deleted."));
    }
    @GetMapping("/loads") public List<ConsumerLoad> getLoads(){ return loads.findAll(); }
    @PostMapping("/loads") public ResponseEntity<?> addLoad(@RequestBody ConsumerLoad load) {
        if (load.getName()==null || load.getName().isBlank() || load.getDemandKwh()<0 ||
            load.getMinimumServiceKwh()<0 || load.getMinimumServiceKwh()>load.getDemandKwh() || load.getPriority()<1 || load.getPriority()>3)
            return ResponseEntity.badRequest().body(Map.of("error","Check load name, demand, minimum service and priority (1-3)."));
        int nextOrder = loads.findAll().stream().mapToInt(ConsumerLoad::getRequestOrder).max().orElse(0) + 1;
        load.setName(load.getName().trim()); load.setId(null); load.setRequestOrder(nextOrder);
        return ResponseEntity.ok(loads.save(load));
    }
    @PutMapping("/loads/{id}") public ResponseEntity<?> updateLoad(@PathVariable Long id, @RequestBody ConsumerLoad load) {
        if (load.getName()==null || load.getName().isBlank() || load.getDemandKwh()<0 || load.getMinimumServiceKwh()<0 || load.getMinimumServiceKwh()>load.getDemandKwh() || load.getPriority()<1 || load.getPriority()>3)
            return ResponseEntity.badRequest().body(Map.of("error","Check name, demand, minimum service and priority (1-3)."));
        return loads.findById(id).map(existing -> {
            existing.setName(load.getName().trim()); existing.setCategory(load.getCategory()); existing.setDemandKwh(load.getDemandKwh());
            existing.setMinimumServiceKwh(load.getMinimumServiceKwh()); existing.setDivisible(load.isDivisible()); existing.setPriority(load.getPriority());
            return ResponseEntity.ok(loads.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @DeleteMapping("/loads/{id}") public ResponseEntity<?> deleteLoad(@PathVariable Long id) {
        if (!loads.existsById(id)) return ResponseEntity.notFound().build();
        loads.deleteById(id); return ResponseEntity.ok(Map.of("message","Consumer load deleted."));
    }
    @PostMapping("/allocations/run") public ResponseEntity<?> run(@RequestBody Map<String,Object> body) {
        try {
            int interval=((Number)body.get("intervalNumber")).intValue();
            double generation=((Number)body.get("generationKwh")).doubleValue();
            if (!Double.isFinite(generation) || generation < 0)
                return ResponseEntity.badRequest().body(Map.of("error","Available generation must be a non-negative number."));
            double totalCapacity = sources.findAll().stream().mapToDouble(EnergySource::getCapacityKwh).sum();
            if (Math.abs(generation - totalCapacity) >= 0.05)
                return ResponseEntity.badRequest().body(Map.of("error", String.format(Locale.ROOT,
                    "Available generation must equal total rated capacity (%.1f kWh).", totalCapacity)));
            return ResponseEntity.ok(service.runAllocation(interval,generation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()==null?"Invalid request":e.getMessage()));
        }
    }
    @GetMapping("/allocations") public List<AllocationRecord> history(){ return records.findAllByOrderByIntervalNumberAscIdAsc(); }
    @DeleteMapping("/allocations") public ResponseEntity<?> clear(){ service.clearHistory(); return ResponseEntity.ok(Map.of("message","Allocation history cleared.")); }
}
