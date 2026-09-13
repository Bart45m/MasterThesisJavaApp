/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.masterthesis.master_thesis_java_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author barto
 */
@RestController
@RequestMapping("/api/Vulnerable")
public class VulnerableController {

    // PODATNOŚĆ 1: Hardcoded Secret (Twardo zakodowane klucze/hasła)
    // SAST natychmiast powinien to wykryć jako Critical/High Issue.
    private static final String ADMIN_API_KEY = "AIzaSyD-TEST-KEY-99283112-Xyz7788A";
    private static final String CONNECTION_STRING = "jdbc:sqlserver://myServerAddress;databaseName=myDataBase;user=admin;password=SuperSecretPassword123!;";

    /**
     * 1. SQL INJECTION (Podatność na wstrzykiwanie kodu SQL)
     */
    @GetMapping("/search-users")
    public ResponseEntity<Map<String, Object>> getUsers(@RequestParam String searchTerm) {
        // BŁĄD SAST: Bezpośrednie łączenie stringów (String Concatenation) zamiast użycia parametrów PreparedStatement.
        String query = "SELECT * FROM Users WHERE UserName = '" + searchTerm + "'";

        try {
            // Kod dla celów SAST (narzędzie analizuje sam przepływ danych ze źródła 'searchTerm' do sinka 'execute')
            Connection connection = DriverManager.getConnection(CONNECTION_STRING);
            Statement statement = connection.createStatement();
            // Przepływ danych (Taint Analysis) zatrzyma się na podatnej metodzie:
            // statement.execute(query); 

            Map<String, Object> response = new HashMap<>();
            response.put("Message", "Wykonano podatne zapytanie: " + query);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 2. PATH TRAVERSAL / ARBITRARY FILE READ (Nieautoryzowany dostęp do plików systemowych)
     */
    @GetMapping("/read-log")
    public ResponseEntity<Map<String, Object>> readLogFile(@RequestParam String fileName) {
        // BŁĄD SAST: Brak walidacji i oczyszczania (sanitization) ścieżki.
        // Użytkownik może przekazać: fileName = "../../../etc/passwd" lub "../../../windows/win.ini"
        String baseDir = System.getProperty("user.dir");
        File fullPath = new File(baseDir + File.separator + "Logs", fileName);

        // Narzędzie SAST wykryje, że nie sprawdzono, czy pobrana ścieżka (canonical path) nie wychodzi poza katalog "Logs"
        if (fullPath.exists()) {
            Map<String, Object> response = new HashMap<>();
            response.put("FilePath", fullPath.getAbsolutePath());
            response.put("Content", "Zawartość pliku logów...");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 3. WEAK CRYPTOGRAPHY (Użycie słabych algorytmów haszujących)
     */
    @PostMapping("/hash-password")
    public ResponseEntity<Map<String, Object>> hashPassword(@RequestParam String password) {
        // BŁĄD SAST: MD5 jest uznawany za złamany i podatny na kolizje.
        // SAST powinien zgłosić regułę typu "Do not use weak cryptographic algorithms" (np. MD5/SHA-1).
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] inputBytes = password.getBytes(StandardCharsets.US_ASCII);
            byte[] hashBytes = md5.digest(inputBytes);

            // Konwersja bajtów na ciąg Hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("PasswordHash", hexString.toString().toUpperCase());
            return ResponseEntity.ok(response);

        } catch (NoSuchAlgorithmException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
