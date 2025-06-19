import com.hades.paie1.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public  class AdminDataSeeder implements CommandLineRunner {

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
}