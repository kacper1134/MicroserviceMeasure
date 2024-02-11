import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class JavaFileReader {
    public static String extractInstructionFromLine(String filePath, int startLineNumber) {
        StringBuilder extractedString = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int currentLineNumber = 1;

            while ((line = reader.readLine()) != null && currentLineNumber < startLineNumber) {
                currentLineNumber++;
            }

            while (line != null) {
                extractedString.append(line.trim());
                if (line.contains(";")) {
                    extractedString.delete(extractedString.indexOf(";") + 1, extractedString.length());
                    break;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractedString.toString();
    }

    public static String getFullClassNameFromImportStatement(String simpleClassName, String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("import") && line.contains(simpleClassName)) {
                    return line.substring(line.indexOf("import") + 7, line.indexOf(";")).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getValueOfField(String filePath, String fieldName) {
        if(isEnum(filePath)) {
            return getValueOfEnumField(filePath, fieldName);
        }
        return getValueOfClassField(filePath, fieldName);
    }

    public static ArrayList<String> getStaticImportClasses(String filePath) {
        ArrayList<String> staticImportClasses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("import static")) {
                    staticImportClasses.add(line.substring(line.indexOf("import static") + 14, line.indexOf(".*;")).trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return staticImportClasses;
    }

    private static String getValueOfEnumField(String filePath, String fieldName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(fieldName)) {
                    return line.substring(line.indexOf(fieldName) + fieldName.length() + 1, line.indexOf(")")).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getValueOfClassField(String filePath, String fieldName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(fieldName)) {
                    int startIndex = line.indexOf('=') + 1;
                    int endIndex = line.indexOf(';');
                    if (endIndex != -1) {
                        return line.substring(startIndex, endIndex).trim();
                    }
                }
            }
        } catch (IOException | StringIndexOutOfBoundsException e) {
            return null;
        }
        return null;
    }

    private static boolean isEnum(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("enum")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
