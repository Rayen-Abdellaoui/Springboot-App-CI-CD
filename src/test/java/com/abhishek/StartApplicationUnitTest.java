package com.abhishek;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

class StartApplicationUnitTest {

    @Test
    void testIndexMethodDirectly() {
        StartApplication controller = new StartApplication();
        Model model = new ConcurrentModel();

        String view = controller.index(model);

        assertEquals("index", view);
        assertTrue(model.containsAttribute("title"));
        assertTrue(model.containsAttribute("msg"));
        assertEquals("This is a SpringBoot Static Web Application", model.getAttribute("title"));
        assertEquals("Application Is Deployed To Kuberneets", model.getAttribute("msg"));
    }
}
