package org.com.belog

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BelogApplicationTests {
    @Test
    fun contextLoads() = Unit
}
