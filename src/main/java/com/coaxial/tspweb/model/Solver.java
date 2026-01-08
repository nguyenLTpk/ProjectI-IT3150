package com.coaxial.tspweb.model;

import com.coaxial.tspweb.common.StatusCode;
import com.coaxial.tspweb.io.SessionWorker;
import com.coaxial.tspweb.io.reqRep.ClientRequest;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Phiên bản Solver Final (Khắc phục lỗi treo 99%).
 * Chiến lược:
 * - N < 10: Chạy ngay lập tức, không tạo luồng Update (Tránh Race Condition).
 * - N >= 10: Chạy với luồng Update tiến độ.
 * - N > 12: Chạy thuật toán Tham lam (Greedy) để đảm bảo tốc độ.
 */
public class Solver {
    private final Logger log = LoggerFactory.getLogger(Solver.class);

    private static final int THRESHOLD_NO_UPDATE = 10; // Dưới 10 điểm chạy siêu nhanh -> ko cần update
    private static final int MAX_EXACT_CITIES = 12;    // Trên 12 điểm -> dùng Greedy

    private long allPaths;
    private long currentDonePaths;
    private AtomicLong globalBestConsumption;
    
    // Cờ kiểm soát luồng update an toàn hơn interrupt()
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SolverResult exactSolve(ClientRequest request, SessionWorker sessionWorker) {
        try {
            // 1. CHUẨN BỊ DỮ LIỆU
            LatLng[] places = request.getLocations().toArray(new LatLng[0]);
            int n = places.length;

            if (n < 2) {
                sessionWorker.tellStatus(StatusCode.ERROR, "Cần ít nhất 2 điểm.");
                return null;
            }

            globalBestConsumption = new AtomicLong(Long.MAX_VALUE);
            currentDonePaths = 0;

            sessionWorker.tellStatus(StatusCode.CALCULATING, "Đang đo đạc khoảng cách...");
            DistanceMatrixElement[][] distances = MapEngine.getDistances(places, TravelMode.DRIVING);
            
            if (distances == null) {
                sessionWorker.tellStatus(StatusCode.ERROR, "Lỗi tính khoảng cách.");
                return null;
            }

            double[] elevationsRaw = MapEngine.getElevations(places);
            final double[] elevations = (elevationsRaw != null) ? elevationsRaw : new double[n];
            double[][] consumptionMatrix = EnergyEngine.getEnergyConsumptionMatrix(distances, elevations, request.getEnergy());

            // Fix lỗi NaN
            for(int i=0; i<n; i++) 
                for(int j=0; j<n; j++) 
                    if (Double.isNaN(consumptionMatrix[i][j])) consumptionMatrix[i][j] = 0;

            PathConsumption bestResult;
            long startTime = System.nanoTime();
            isRunning.set(true);

            // 2. CHỌN CHIẾN LƯỢC
            if (n > MAX_EXACT_CITIES) {
                // --- CHẾ ĐỘ SIÊU TỐC (GREEDY) ---
                sessionWorker.tellStatus(StatusCode.CALCULATING, "Dữ liệu lớn (" + n + "). Chạy chế độ Tốc độ...", 50);
                bestResult = solveGreedy(consumptionMatrix, distances);
                
            } else {
                // --- CHẾ ĐỘ CHÍNH XÁC (EXACT) ---
                allPaths = faq(n - 1);

                // Chỉ tạo luồng cập nhật nếu số lượng tính toán đủ lớn
                Thread updater = null;
                if (n >= THRESHOLD_NO_UPDATE) {
                    updater = new Thread(() -> {
                        while (isRunning.get()) {
                            double progress = (allPaths > 0) ? ((double) currentDonePaths / allPaths) * 100.0 : 0;
                            if (progress > 99) progress = 99;
                            sessionWorker.tellStatus(StatusCode.CALCULATING, 
                                    "Đang duyệt " + currentDonePaths + "/" + allPaths + " lộ trình...", Math.round(progress));
                            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
                        }
                    });
                    updater.start();
                }

                try {
                    // Sắp xếp heuristic
                    int[][] index = new int[n][];
                    for (int i = 0; i < n; ++i) {
                        ElementIndexComparator comp = new ElementIndexComparator(distances[i]);
                        Integer[] indexLocal = comp.createIndexArray();
                        Arrays.sort(indexLocal, comp);
                        int[] toAdd = new int[indexLocal.length];
                        for (int j = 0; j < toAdd.length; ++j) toAdd[j] = indexLocal[j];
                        index[i] = toAdd;
                    }

                    // Chạy đệ quy trên luồng chính
                    ArrayList<Integer> startPath = new ArrayList<>();
                    startPath.add(0);
                    bestResult = recExact(new PathConsumption(0, 0, startPath), consumptionMatrix, distances, index);
                    
                } finally {
                    // Tắt luồng update an toàn
                    isRunning.set(false);
                    if (updater != null) {
                        updater.interrupt();
                        try { updater.join(1000); } catch (InterruptedException ignored) {}
                    }
                }
            }

            long totalTime = System.nanoTime() - startTime;
            return packResult(bestResult, distances, elevations, consumptionMatrix, sessionWorker, totalTime);

        } catch (Exception e) {
            e.printStackTrace();
            sessionWorker.tellStatus(StatusCode.ERROR, "Lỗi: " + e.getMessage());
            return null;
        }
    }

    // --- LOGIC ĐỆ QUY ---
    private PathConsumption recExact(PathConsumption current, double[][] costMatrix, DistanceMatrixElement[][] distances, int[][] index) {
        int curPlace = current.path.get(current.path.size() - 1);

        if (current.path.size() == costMatrix.length) {
            current.path.add(0);
            current.consumption += costMatrix[curPlace][0];
            current.distance += distances[curPlace][0].distance.inMeters;
            currentDonePaths++;
            return current;
        } 
        
        if (current.consumption > globalBestConsumption.get()) {
            currentDonePaths += faq(costMatrix.length - current.path.size());
            return current; 
        }

        PathConsumption localBest = new PathConsumption(Long.MAX_VALUE, 0, null);

        for (int k = 0; k < costMatrix[curPlace].length; ++k) {
            int nextPlaceIndex = index[curPlace][k];
            if (current.path.contains(nextPlaceIndex)) continue;

            PathConsumption nextStep = new PathConsumption(current.consumption, current.distance, new ArrayList<>(current.path));
            nextStep.path.add(nextPlaceIndex);
            nextStep.consumption += costMatrix[curPlace][nextPlaceIndex];
            nextStep.distance += distances[curPlace][nextPlaceIndex].distance.inMeters;

            nextStep = recExact(nextStep, costMatrix, distances, index);

            if (nextStep.consumption < localBest.consumption) {
                localBest = nextStep;
                long val = nextStep.consumption;
                globalBestConsumption.updateAndGet(currentBest -> Math.min(currentBest, val));
            }
        }
        return localBest;
    }

    // --- LOGIC THAM LAM ---
    private PathConsumption solveGreedy(double[][] costMatrix, DistanceMatrixElement[][] distances) {
        int n = costMatrix.length;
        ArrayList<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        int current = 0;
        path.add(current);
        visited[current] = true;
        long totalCost = 0;
        long totalDist = 0;

        for (int i = 0; i < n - 1; i++) {
            int next = -1;
            double minCost = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (!visited[j] && costMatrix[current][j] < minCost) {
                    minCost = costMatrix[current][j];
                    next = j;
                }
            }
            if (next != -1) {
                visited[next] = true;
                path.add(next);
                totalCost += costMatrix[current][next];
                totalDist += distances[current][next].distance.inMeters;
                current = next;
            }
        }
        path.add(0);
        totalCost += costMatrix[current][0];
        totalDist += distances[current][0].distance.inMeters;
        return new PathConsumption(totalCost, totalDist, path);
    }

    // --- HELPER ---
    private SolverResult packResult(PathConsumption result, DistanceMatrixElement[][] distances, double[] elevations, 
                                    double[][] consumptionMatrix, SessionWorker sessionWorker, long time) {
        sessionWorker.tellStatus(StatusCode.DONE); // Gửi DONE đè lên status cũ

        List<Long> singleDistances = new ArrayList<>();
        List<Double> singleConsumptions = new ArrayList<>();
        if (result.path != null && result.path.size() > 1) {
             singleDistances = IntStream.range(1, result.path.size())
                .mapToObj(i -> distances[result.path.get(i-1)][result.path.get(i)].distance.inMeters)
                .collect(Collectors.toList());
             singleConsumptions = IntStream.range(1, result.path.size())
                .mapToObj(i -> consumptionMatrix[result.path.get(i-1)][result.path.get(i)])
                .collect(Collectors.toList());
        }

        return new SolverResult(result.path, elevations, singleDistances, singleConsumptions,
                result.distance, time, allPaths, 0, allPaths, (result.consumption * 1D));
    }

    private long faq(int n) {
        if (n <= 1) return 1;
        long r = 1;
        for (int i = 2; i <= n; ++i) r *= i;
        return r;
    }

    private static class PathConsumption {
        long consumption;
        long distance;
        ArrayList<Integer> path;
        PathConsumption(long c, long d, ArrayList<Integer> p) { consumption = c; distance = d; path = p; }
    }
}