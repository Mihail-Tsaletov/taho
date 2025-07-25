package svaga.taho.model;

import org.springframework.stereotype.Component;

@Component
public class User {
    private String userId;
    private String email;
    private String name;
    private String phoneNumber;

    public User() {
    }

    public User(String userId, String phoneNumber, String name, String email) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
