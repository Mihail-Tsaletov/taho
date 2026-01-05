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

    // Метод для расчёта минимальной цены
    public double calculateMinPrice(String startPoint, String endPoint) {
        String startDistrict = getDistrictForPoint(parseLon(startPoint), parseLat(startPoint));
        String endDistrict = getDistrictForPoint(parseLon(endPoint), parseLat(endPoint));

        // Если хотя бы одна точка вне районов — внешняя цена
        if ("Outside".equals(startDistrict) || "Outside".equals(endDistrict)) {
            return 300.0;
        }

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

    // Вспомогательные для парсинга "lon, lat"
    public double parseLon(String point) {
        return Double.parseDouble(point.split(",")[0].trim());
    }

    public double parseLat(String point) {
        return Double.parseDouble(point.split(",")[1].trim());
    }
}