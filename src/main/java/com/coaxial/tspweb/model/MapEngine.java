package com.coaxial.tspweb.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * MapEngine Hybrid:
 * - Ưu tiên dùng API Online miễn phí (routing.openstreetmap.de).
 * - Tự động chuyển sang Offline (Haversine) nếu API lỗi hoặc quá tải.
 * - KHÔNG CẦN DOCKER.
 */
public class MapEngine {

    // Server OSRM của Đức (Ổn định hơn server gốc project-osrm)
    private static final String OSRM_API_URL = "https://routing.openstreetmap.de/routed-car/table/v1/driving/";
    private static final double EARTH_RADIUS = 6371000;
    
    // GIỚI HẠN: Nếu chọn > 15 điểm, dùng Offline luôn để tránh làm treo server
    private static final int MAX_ONLINE_POINTS = 15; 

    public static DistanceMatrixElement[][] getDistances(LatLng[] locations, TravelMode mode) {
        int size = locations.length;
        
        // 1. NẾU QUÁ NHIỀU ĐIỂM -> CHẠY OFFLINE NGAY
        if (size > MAX_ONLINE_POINTS) {
            System.out.println("LOG: Số điểm lớn (" + size + ") -> Chuyển sang chế độ Offline để đảm bảo tốc độ.");
            return getDistancesOffline(locations);
        }

        // 2. NẾU ÍT ĐIỂM -> THỬ GỌI API
        try {
            // Làm tròn tọa độ 5 chữ số để tận dụng Cache của Server (Tránh timeout)
            DecimalFormat df = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.US));
            StringBuilder coords = new StringBuilder();
            for (int i = 0; i < size; i++) {
                coords.append(df.format(locations[i].lng))
                      .append(",")
                      .append(df.format(locations[i].lat));
                if (i < size - 1) coords.append(";");
            }

            String url = OSRM_API_URL + coords.toString() + "?annotations=distance";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4)) // Chỉ chờ tối đa 4 giây
                    .build();

            // Giả lập trình duyệt Chrome để không bị chặn
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse kết quả nếu thành công
                DistanceMatrixElement[][] result = parseOSRMResponse(response.body(), size);
                if (result != null) return result;
            } else {
                System.err.println("LOG: Server API bận hoặc từ chối (Code: " + response.statusCode() + ")");
            }

        } catch (Exception e) {
            // Lỗi mạng hoặc Timeout -> Không làm gì cả, để nó trôi xuống phần fallback bên dưới
            System.err.println("LOG: Không kết nối được API: " + e.getMessage());
        }

        // 3. FALLBACK CUỐI CÙNG: DÙNG OFFLINE
        // Đảm bảo chương trình KHÔNG BAO GIỜ CHẾT dù mất mạng
        System.out.println("LOG: Đang tính toán bằng thuật toán Offline (Haversine)...");
        return getDistancesOffline(locations);
    }

    private static DistanceMatrixElement[][] parseOSRMResponse(String jsonBody, int size) {
        DistanceMatrixElement[][] matrix = new DistanceMatrixElement[size][size];
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(jsonBody, JsonObject.class);

            if (json.has("code") && !"Ok".equals(json.get("code").getAsString())) return null;
            if (!json.has("distances")) return null;

            JsonArray distancesArr = json.getAsJsonArray("distances");

            for (int i = 0; i < size; i++) {
                JsonArray row = distancesArr.get(i).getAsJsonArray();
                matrix[i] = new DistanceMatrixElement[size];
                for (int j = 0; j < size; j++) {
                    DistanceMatrixElement element = new DistanceMatrixElement();
                    Distance dist = new Distance();
                    // OSRM trả về mét -> Ép kiểu sang long
                    dist.inMeters = (long) row.get(j).getAsDouble();
                    element.distance = dist;
                    element.status = DistanceMatrixElementStatus.OK;
                    matrix[i][j] = element;
                }
            }
            return matrix;
        } catch (Exception e) {
            return null;
        }
    }

    // --- TÍNH TOÁN KHOẢNG CÁCH ĐƯỜNG CHIM BAY (OFFLINE) ---
    public static DistanceMatrixElement[][] getDistancesOffline(LatLng[] locations) {
        int size = locations.length;
        DistanceMatrixElement[][] matrix = new DistanceMatrixElement[size][size];
        for (int i = 0; i < size; i++) {
            matrix[i] = new DistanceMatrixElement[size];
            for (int j = 0; j < size; j++) {
                DistanceMatrixElement element = new DistanceMatrixElement();
                Distance dist = new Distance();
                if (i == j) dist.inMeters = 0;
                else dist.inMeters = calculateHaversineDistance(locations[i].lat, locations[i].lng, locations[j].lat, locations[j].lng);
                element.distance = dist;
                element.status = DistanceMatrixElementStatus.OK;
                matrix[i][j] = element;
            }
        }
        return matrix;
    }

    // Công thức Haversine (Toán học)
    private static long calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (long) (EARTH_RADIUS * c);
    }
    
    // Các hàm phụ trợ giữ nguyên
    public static double[] getElevations(LatLng[] places) { return new double[places.length]; }
    public static DistanceMatrixElement[][] getDistances(String[] locations, TravelMode mode) { return null; }
}