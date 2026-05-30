package com.store.ecommerce;

import com.paypal.sdk.PaypalServerSdkClient;
import com.store.ecommerce.config.TestCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
@EnableAutoConfiguration(exclude = {
		RedisAutoConfiguration.class,
		RedisRepositoriesAutoConfiguration.class
})
class EcommerceApplicationTests {

	@MockBean
	private PaypalServerSdkClient paypalClient;

	@Test
	void contextLoads() {
	}
}
