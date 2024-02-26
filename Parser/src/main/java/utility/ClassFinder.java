package utility;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ClassFinder {
    public static List<String> findClassFiles(String directoryPath) throws IOException {
        List<String> classFiles = new ArrayList<>();
        Path start = Path.of(directoryPath);

        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".class")) {
                    classFiles.add(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return classFiles;
    }
}
