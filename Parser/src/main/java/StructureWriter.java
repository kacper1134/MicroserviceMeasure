import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class StructureWriter {
    private static final Logger LOG = LogManager.getLogger(StructureWriter.class);

    public static void deleteFilesInDirectory(String projectDirectory) {
        try {
            Path directory = Paths.get(projectDirectory);

            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeAboutClassMethodsInfo(String projectName, String microserviceName, ArrayList<String> methodInfo) {
        String sheetName = "classMethods";
        String[] headers = {"Class Name", "Method Signature", "Method Modifier", "Return Type"};
        writeInfoToFile(projectName, microserviceName, methodInfo, sheetName, headers);
        LOG.info("Class methods info written to file.");
    }

    public static void writeAboutInterfaceMethodsInfo(String projectName, String microserviceName, ArrayList<String> methodInfo) {
        String sheetName = "interfaceOperations";
        String[] headers = {"Interface Name", "Operation Signature", "Operation Modifier", "Return Type"};
        writeInfoToFile(projectName, microserviceName, methodInfo, sheetName, headers);
        LOG.info("Interface methods info written to file.");
    }

    public static void writeAboutFieldsInfo(String projectName, String microserviceName, ArrayList<String> fieldsInfo) {
        String sheetName = "classFields";
        String[] headers = {"Class Name", "Field Name", "Field Modifier", "Field Type"};
        writeInfoToFile(projectName, microserviceName, fieldsInfo, sheetName, headers);
        LOG.info("Fields info written to file.");
    }

    public static void writeAboutClassToClassRelations(String projectName, String microserviceName, HashMap<String, Integer> classRelations) {
        ArrayList<String> structureInfo = classRelations.entrySet().stream()
                .map(entry -> entry.getKey() + "||" + entry.getValue())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        String sheetName = "classToClassRelations";
        String[] headers = {"Source Class Name", "Source Method Signature", "Target Class Name", "Target Method Signature", "Weight"};
        writeInfoToFile(projectName, microserviceName, structureInfo, sheetName, headers);
        LOG.info("Class to class relations info written to file.");
    }

    public static void writeAboutClassToInterfaceRelations(String projectName, String microserviceName, HashMap<String, Integer> classRelations) {
        ArrayList<String> structureInfo = classRelations.entrySet().stream()
                .map(entry -> entry.getKey() + "||" + entry.getValue())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        String sheetName = "classToInterfaceRelations";
        String[] headers = {"Source Class Name", "Source Method Signature", "Target Interface Name", "Target Operation Signature", "Weight"};
        writeInfoToFile(projectName, microserviceName, structureInfo, sheetName, headers);
        LOG.info("Class to interface relations info written to file.");
    }

    public static void writeAboutInterfaceToClassRelations(String projectName, String microserviceName, HashMap<String, Integer> interfaceRelations) {
        ArrayList<String> structureInfo = interfaceRelations.entrySet().stream()
                .map(entry -> entry.getKey() + "||" + entry.getValue())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        String sheetName = "interfaceToClassRelations";
        String[] headers = {"Source Interface Name", "Source Operation Signature", "Target Class Name", "Target Method Signature", "Weight"};
        writeInfoToFile(projectName, microserviceName, structureInfo, sheetName, headers);
        LOG.info("Interface to class relations info written to file.");
    }

    public static void writeAboutInterfaceToInterfaceRelations(String projectName, String microserviceName, HashMap<String, Integer> interfaceRelations) {
        ArrayList<String> structureInfo = interfaceRelations.entrySet().stream()
                .map(entry -> entry.getKey() + "||" + entry.getValue())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        String sheetName = "interfaceToInterfaceRelations";
        String[] headers = {"Source Interface Name", "Source Operation Signature", "Target Interface Name", "Target Operation Signature", "Weight"};
        writeInfoToFile(projectName, microserviceName, structureInfo, sheetName, headers);
        LOG.info("Interface to interface relations info written to file.");
    }

    public static void writeInfoToFile(String projectName, String microserviceName, ArrayList<String> structureInfo, String sheetName, String[] headers) {
        String filePath = "src/main/resources/" + projectName + "/" + microserviceName + "_structure2.xlsx";
        createDirectoryIfNotExists(filePath);

        try (Workbook workbook = getWorkbook(filePath)) {
            Sheet sheet = getOrCreateSheet(workbook, sheetName, headers);

            int lastRowNum = sheet.getLastRowNum();
            int rowNum = lastRowNum >= 0 ? lastRowNum + 1 : 0;

            for (String info : structureInfo) {
                String[] parts = info.split("\\|\\|");
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < parts.length; i++) {
                    row.createCell(i).setCellValue(parts[i]);
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDirectoryIfNotExists(String filePath) {
        File directory = new File(filePath).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private static Workbook getWorkbook(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            return new XSSFWorkbook(new FileInputStream(file));
        } else {
            return new XSSFWorkbook();
        }
    }

    private static Sheet getOrCreateSheet(Workbook workbook, String sheetName, String[] headers) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
        }
        return sheet;
    }

}
