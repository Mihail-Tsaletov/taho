package svaga.taho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ResourceLoader;
import svaga.taho.repository.IBasePricesRepository;

import java.io.InputStream;
import java.util.*;

@Service
public class DistrictService {
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<Polygon> districts = new ArrayList<>();
    private final List<String> districtNames = new ArrayList<>(); // ← имена районов

    @Autowired
    private ResourceLoader resourceLoader;
    private final IBasePricesRepository basePrices;

    public DistrictService(IBasePricesRepository basePrices) {
        this.basePrices = basePrices;
    }

    @PostConstruct
    public void init() throws Exception {
        InputStream is = resourceLoader.getResource("classpath:districts.geojson").getInputStream();
        JsonNode root = mapper.readTree(is);
        JsonNode features = root.get("features");

        for (JsonNode feature : features) {
            JsonNode properties = feature.get("properties");
            String districtName = properties.has("name")
                    ? properties.get("name").asText()
                    : "Неизвестный район";

            JsonNode coordinates = feature.get("geometry").get("coordinates").get(0);

            Coordinate[] coords = new Coordinate[coordinates.size() + 1];
            for (int i = 0; i < coordinates.size(); i++) {
                JsonNode point = coordinates.get(i);
                coords[i] = new Coordinate(point.get(0).asDouble(), point.get(1).asDouble());
            }
            coords[coordinates.size()] = coords[0]; // закрываем полигон

            Polygon polygon = geometryFactory.createPolygon(coords);
            districts.add(polygon);
            districtNames.add(districtName); // ← сохраняем имя
        }
    }

    public String getDistrictForPoint(double lon, double lat) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

        for (int i = 0; i < districts.size(); i++) {
            if (districts.get(i).contains(point)) {
                return districtNames.get(i); // ← возвращаем имя!
            }
        }
        return "Outside";
    }

    public boolean isInCity(String startPoint, String endPoint) {
        String startDistrict = getDistrictForPoint(parseLon(startPoint), parseLat(startPoint));
        String endDistrict = getDistrictForPoint(parseLon(endPoint), parseLat(endPoint));

        // Если хотя бы одна точка вне районов — внешняя цена
        if ("Outside".equals(startDistrict) || "Outside".equals(endDistrict)) {
            return false;
        } else {
            return true;
        }
    }

    // Метод для расчёта минимальной цены
    // используется только при заказе в города
    public double calculateMinPrice(String startPoint, String endPoint) {
        String startDistrict = getDistrictForPoint(parseLon(startPoint), parseLat(startPoint));
        String endDistrict = getDistrictForPoint(parseLon(endPoint), parseLat(endPoint));

        if (startDistrict.equals(endDistrict)) {
            return 100.0; // Внутри района
        } else if (startDistrict.equals("Двадцать вторая") && endDistrict.equals("Восемнадцатая")) {
            return 200.0; // Между 1 и 2
        } else if (startDistrict.equals("Восемнадцатая") && endDistrict.equals("Двадцать вторая")) {
            return 200.0; // Обратно
        } else {
            return 300.0; // Вне районов или неизвестно
        }
    }

    public double calculateRealDistance(String trackJson) throws Exception {
        JsonNode points = mapper.readTree(trackJson);
        double totalMeters = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            JsonNode p1 = points.get(i);
            JsonNode p2 = points.get(i + 1);

            double lon1 = p1.get(0).asDouble();
            double lat1 = p1.get(1).asDouble();
            double lon2 = p2.get(0).asDouble();
            double lat2 = p2.get(1).asDouble();

            totalMeters += haversine(lat1, lon1, lat2, lon2);
        }

        return totalMeters / 1000.0; // км
    }

    // Haversine formula (расстояние по прямой между двумя точками)
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371_000; // радиус Земли в метрах

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // Вспомогательные для парсинга "lon, lat"
    public double parseLon(String point) {
        return Double.parseDouble(point.split(",")[0].trim());
    }

    public double parseLat(String point) {
        return Double.parseDouble(point.split(",")[1].trim());
    }
}