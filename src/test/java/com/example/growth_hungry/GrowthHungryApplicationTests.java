package com.example.growth_hungry;


import com.example.growth_hungry.security.JwtAuthFilter;
import com.example.growth_hungry.security.JwtUtil;
import com.example.growth_hungry.security.Json401EntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				// Самый простой способ отключить тяжёлые автоконфигурации в тесте:
				"spring.autoconfigure.exclude=" +
						"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
						"org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
						"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
						"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
		}
)
@ActiveProfiles("test")
class GrowthHungryApplicationTests {

	// Если где-то есть @Autowired на эти бины, замокаем на всякий случай.
	@MockBean JwtAuthFilter jwtAuthFilter;
	@MockBean JwtUtil jwtUtil;
	@MockBean Json401EntryPoint json401EntryPoint;

	@Test
	void contextLoads() {
		// smoke: контекст должен подняться без ошибок
	}
}


