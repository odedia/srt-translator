package com.odedia.translator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.stereotype.Service;

@Service
public class FileService {

    public void writeStringToFile(String content, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        // Check if file exists, if not create it
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        Files.write(path, content.getBytes());
    }
    
    public void appendStringToFile(String content, String filePath) throws IOException {
    	Path path = Paths.get(filePath);
        // Check if file exists, if not create it
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

    	Files.write(path, content.getBytes(), StandardOpenOption.APPEND);
    }

}
