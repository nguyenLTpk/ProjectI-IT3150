package com.coaxial.tspweb.model;


import com.coaxial.tspweb.io.GsonWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Chứa tất cả các yếu tố của một kết quả quá trình giải quyết.
 */
public class SolverResult
{
    private List<Integer> path;
    private Double[] elevations;
    private List<Long> singleDistances;
    private List<Double> singleConsumptions;
    private long distance;
    private long calculationTime;
    private long completelyCalculatedPaths;
    private long incompletelyCalculatedPaths;
    private long possiblePaths;
    private double bestPathConsumption;

    public SolverResult(List<Integer> path, double[] elevations, List<Long> singleDistances,
                        List<Double> singleConsumptions, long distance, long calculationTime, long completelyCalculatedPaths,
                        long incompletelyCalculatedPaths, long possiblePaths, double bestPathConsumption)
    {
        this.path = path;
        this.elevations = new Double[elevations.length];
        for(int i=0; i<elevations.length; ++i)
            this.elevations[i] = elevations[i];
        this.singleDistances = singleDistances;
        this.singleConsumptions = singleConsumptions;
        this.distance = distance;
        this.calculationTime = calculationTime;
        this.completelyCalculatedPaths = completelyCalculatedPaths;
        this.incompletelyCalculatedPaths = incompletelyCalculatedPaths;
        this.possiblePaths = possiblePaths;
        this.bestPathConsumption = bestPathConsumption;
    }

    public GsonWrapper toGsonWrapper(GsonWrapper source)
    {
        if(source == null)
            source = new GsonWrapper();
        return source
                .add("path",path.toArray(new Integer[]{}))
                .add("distance", distance)
                .add("singleDistances", singleDistances.toArray(new Long[]{}))
                .add("singleConsumptions", singleConsumptions.toArray(new Double[]{}))
                .add("elevations", elevations)
                .add("calculationTime", calculationTime)
                .add("completelyCalculatedPaths", completelyCalculatedPaths)
                .add("incompletelyCalculatedPaths", incompletelyCalculatedPaths)
                .add("possiblePaths", possiblePaths)
                .add("bestPathConsumption", bestPathConsumption);
    }

    public GsonWrapper toGsonWrapper()
    {
        return toGsonWrapper(null);
    }
}

