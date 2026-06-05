import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "aitty123";   // 这里设置你想要的明文密码
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("密文: " + encodedPassword);
    }
}