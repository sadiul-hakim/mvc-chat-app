package xyz.sadiulhakim;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class MvcChatAppApplicationTests {

    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void contextLoads() {
        modules.verify();
    }

}
