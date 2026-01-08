package com.coaxial.tspweb.model;

import com.coaxial.tspweb.io.reqRep.Energy;
import com.google.maps.model.DistanceMatrixElement;

public class EnergyEngine {
    /**
     * Chuyển bài toán thành TSP cổ điển: Chi phí đi lại chính là khoảng cách mét.
     */
    public static double[][] getEnergyConsumptionMatrix(DistanceMatrixElement[][] distances, double[] elevations, Energy energy) {
        double[][] result = new double[distances.length][distances.length];

        for (int i = 0; i < result.length; ++i) {
            for (int j = 0; j < result.length; ++j) {
                // Chi phí = Số mét khoảng cách
                result[i][j] = distances[i][j].distance.inMeters;
            }
        }
        return result;
    }
}