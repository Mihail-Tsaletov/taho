/*
package svaga.taho.service;  // или security, как удобнее

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import svaga.taho.model.User;
import svaga.taho.model.UserRole;
import svaga.taho.repository.IUserRepository;
import svaga.taho.repository.IManagerRepository;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IManagerRepository managerRepository;

    public CustomUserDetailsService(IUserRepository userRepository, IManagerRepository managerRepository) {
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + phone));

        // Проверяем, менеджер ли это
        boolean isManager = managerRepository.existsByUserId(user.getId());

        // Собираем authorities: базовая роль + ROLE_MANAGER если менеджер
        var authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        if (isManager) {
            authorities.add(new SimpleGrantedAuthority(UserRole.MANAGER.name()));
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getPhone())          // username = phone
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
}*/
