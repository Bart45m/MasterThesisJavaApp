package com.masterthesis.master_thesis_java_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class MasterThesisJavaAppApplication {

    public static void main(String[] args) {
        // Włącza tryb headless jako false, aby biblioteka Desktop mogła wywołać przeglądarkę systemową
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(MasterThesisJavaAppApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerUI() {
        // Odpalenie przeglądarki w osobnym wątku chroni proces główny Springa przed wywaleniem
        new Thread(() -> {
            try {
                String url = "http://localhost:8080/swagger-ui/index.html";
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception e) {
                System.err.println("Nie udało się otworzyć przeglądarki: " + e.getMessage());
            }
        }).start();
    }
}
