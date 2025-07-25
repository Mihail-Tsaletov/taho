package svaga.taho.model;

import org.springframework.stereotype.Component;

import com.google.cloud.Timestamp;

@Component
public class Order {
    private String orderId;
    private String clientId;
    private String driverId;
    private String startPoint;
    private String endPoint;
    private boolean isWithinCity = true;
    private String status;
    private Timestamp orderTime;
    private Timestamp assignedTime;
    private Timestamp acceptanceTime;
    private Timestamp pickupTime;
    private Timestamp dropOffTime;
    private Double price;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public boolean isWithinCity() {
        return isWithinCity;
    }

    public void setWithinCity(boolean withinCity) {
        isWithinCity = withinCity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Timestamp orderTime) {
        this.orderTime = orderTime;
    }

    public Timestamp getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(Timestamp assignedTime) {
        this.assignedTime = assignedTime;
    }

    public Timestamp getAcceptanceTime() {
        return acceptanceTime;
    }

    public void setAcceptanceTime(Timestamp acceptanceTime) {
        this.acceptanceTime = acceptanceTime;
    }

    public Timestamp getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(Timestamp pickupTime) {
        this.pickupTime = pickupTime;
    }

    public Timestamp getDropOffTime() {
        return dropOffTime;
    }

    public void setDropOffTime(Timestamp dropOffTime) {
        this.dropOffTime = dropOffTime;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}
