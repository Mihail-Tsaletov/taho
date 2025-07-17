package svaga.taho.model;

import org.springframework.stereotype.Component;

@Component
public class User {
    private String userId;
    private String phoneNumber;
    private String name;
    private String role;

    public User() {
    }

    public User(String userId, String phoneNumber, String name, String role) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
