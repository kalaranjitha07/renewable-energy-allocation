package com.kathir.energy.service;

import com.kathir.energy.model.*;
import com.kathir.energy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AllocationService {
    private final EnergySourceRepository sources;
    private final ConsumerLoadRepository loads;
    private final AllocationRecordRepository records;

    public AllocationService(EnergySourceRepository sources, ConsumerLoadRepository loads, AllocationRecordRepository records) {
        this.sources=sources; this.loads=loads; this.records=records;
    }

    public Map<String,Object> dashboard() {
        List<EnergySource> sourceList=sources.findAll();
        List<ConsumerLoad> loadList=loads.findAll();
        List<AllocationRecord> history=records.findAllByOrderByIntervalNumberAscIdAsc();
        double demand=loadList.stream().mapToDouble(ConsumerLoad::getDemandKwh).sum();
        double lastGeneration=history.isEmpty()?0:history.get(history.size()-1).getGenerationKwh();
        double lastAllocated=0, lastUnmet=0, lastCurtailed=0;
        if (!history.isEmpty()) {
            int lastInterval=history.get(history.size()-1).getIntervalNumber();
            for (AllocationRecord r: history) if (r.getIntervalNumber()==lastInterval) {
                lastAllocated+=r.getAllocatedKwh(); lastUnmet+=r.getUnmetKwh(); lastCurtailed+=r.getCurtailedKwh();
            }
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("sources",sourceList); result.put("loads",loadList); result.put("history",history);
        result.put("totalDemandKwh",round(demand)); result.put("lastGenerationKwh",round(lastGeneration));
        result.put("lastAllocatedKwh",round(lastAllocated)); result.put("lastUnmetKwh",round(lastUnmet));
        result.put("lastCurtailedKwh",round(lastCurtailed)); result.put("intervalCount",history.stream().map(AllocationRecord::getIntervalNumber).distinct().count());
        return result;
    }

    @Transactional
    public Map<String,Object> runAllocation(int interval, double generationKwh) {
        if (interval < 1 || interval > 24) throw new IllegalArgumentException("Interval must be between 1 and 24.");
        if (!Double.isFinite(generationKwh) || generationKwh < 0) throw new IllegalArgumentException("Generation must be a non-negative number.");
        if (records.findAll().stream().anyMatch(r -> r.getIntervalNumber()==interval))
            throw new IllegalArgumentException("This interval already has an allocation. Clear history before rerunning it.");
        List<ConsumerLoad> ordered=new ArrayList<>(loads.findAll());
        ordered.sort(Comparator.comparingInt(ConsumerLoad::getPriority)
                .thenComparingInt(ConsumerLoad::getRequestOrder).thenComparing(ConsumerLoad::getId));
        double remaining=generationKwh, allocatedTotal=0, unmetTotal=0;
        List<AllocationRecord> created=new ArrayList<>();
        for (ConsumerLoad load: ordered) {
            double demand=load.getDemandKwh(), allocation=0;
            if (load.isDivisible()) {
                allocation=Math.min(demand, remaining);
            } else if (remaining >= demand) {
                allocation=demand;
            } else if (load.getMinimumServiceKwh() > 0 && remaining >= load.getMinimumServiceKwh()) {
                allocation=load.getMinimumServiceKwh();
            }
            allocation=round(allocation);
            remaining=round(Math.max(0,remaining-allocation));
            double unmet=round(Math.max(0,demand-allocation));
            AllocationRecord record=new AllocationRecord(interval,load.getId(),load.getName(),load.getCategory(),
                    demand,allocation,unmet,0,generationKwh);
            created.add(records.save(record));
            allocatedTotal+=allocation; unmetTotal+=unmet;
        }
        double curtailed=round(Math.max(0,generationKwh-allocatedTotal));
        // Store curtailed supply consistently on the first record for this interval.
        if (!created.isEmpty() && curtailed > 0) {
            AllocationRecord first=created.get(0);
            records.delete(first);
            AllocationRecord corrected=new AllocationRecord(interval, first.getLoadId(), first.getLoadName(),
                    first.getCategory(), first.getRequestedKwh(), first.getAllocatedKwh(), first.getUnmetKwh(), curtailed, generationKwh);
            created.set(0, records.save(corrected));
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("intervalNumber",interval); result.put("generationKwh",round(generationKwh));
        result.put("allocatedKwh",round(allocatedTotal)); result.put("unmetKwh",round(unmetTotal));
        result.put("curtailedKwh",curtailed); result.put("records",created);
        return result;
    }

    @Transactional
    public void clearHistory() { records.deleteAllInBatch(); }

    private double round(double n) { return Math.round(n*100.0)/100.0; }
}
