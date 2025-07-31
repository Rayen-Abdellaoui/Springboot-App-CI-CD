package com.abhishek;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StartApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndexEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("title", "This is a SpringBoot Static Web Application"))
                .andExpect(model().attribute("msg", "Application Is Deployed To Kuberneets"));
    }

    @Test
    void testMainMethodRuns() {
        StartApplication.main(new String[]{});
    }
}
