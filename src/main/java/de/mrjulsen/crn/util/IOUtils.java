package de.mrjulsen.crn.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class IOUtils {
    public static void writeTextFile(String filePath, String content) throws IOException {
        Files.writeString(Path.of(filePath), content);
    }

    public static String readTextFile(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        return new String(fileBytes, StandardCharsets.UTF_8);
    }
}
