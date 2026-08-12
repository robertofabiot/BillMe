import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.lang.reflect.Constructor;

public class TestDao {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("org.springframework.security.authentication.dao.DaoAuthenticationProvider");
        for(Constructor<?> c : clazz.getConstructors()) {
            System.out.println(c);
        }
    }
}
