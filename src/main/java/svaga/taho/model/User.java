package svaga.taho.model;

import org.springframework.stereotype.Component;

@Component
public class User {
    private String userId;
    private String phoneNumber;
    private String name;
    private String email;
    private String role;

    public User() {
    }

    public User(String userId, String phoneNumber, String name, String role, String email) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.role = role;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
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
