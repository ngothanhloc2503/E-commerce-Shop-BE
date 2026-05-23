package com.store.ecommerce;

import com.paypal.sdk.PaypalServerSdkClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EcommerceApplicationTests {

	@MockBean
	private PaypalServerSdkClient paypalClient;

	@Test
	void contextLoads() {
	}
}
