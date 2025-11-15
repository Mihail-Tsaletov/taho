package svaga.taho.DTO;

import lombok.Data;
import svaga.taho.model.UserRole;


@Data
public class RegisterRequest {
    private String phone;
    private String password;
    private String name;
    private UserRole role = UserRole.CLIENT; // по умолчанию пользователь
}