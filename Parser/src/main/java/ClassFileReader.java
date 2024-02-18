import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassFileReader {
    public static int getNumberOfLinesOfClass(String classContent, String className) {
        className = extractClassName(className);
        classContent = extractClassContent(getClassType(classContent, className), classContent, className);
        classContent = removeEmptyLinesAndAnnotations(classContent);

        return countLines(classContent);
    }

    public static int getNumberOfLinesOfMethod(String classContent, String className, String methodSignature) {
        classContent = removeEmptyLinesAndAnnotations(classContent);
        className = extractClassName(className);
        classContent = extractClassContent(getClassType(classContent, className), classContent, className);

        String methodContent = extractMethodContent(classContent, methodSignature);

        return countLines(methodContent);
    }

    private static String extractClassName(String className) {
        int lastIndex = className.lastIndexOf('$');
        if (lastIndex != -1 && lastIndex < className.length() - 1) {
            return className.substring(lastIndex + 1);
        }
        return className;
    }

    private static String getClassType(String classContent, String className) {
        if (classContent.contains("class " + className)) {
            return "class";
        } else if (classContent.contains("enum " + className)) {
            return "enum";
        } else if (classContent.contains("interface " + className)) {
            return "interface";
        } else {
            return "";
        }
    }

    private static String extractClassContent(String type, String classContent, String className) {
        int startIndex = classContent.indexOf(type + " " + className);

        if (startIndex == -1) {
            return "";
        }

        int endIndex = startIndex;
        int braceCount = 0;
        while (endIndex < classContent.length()) {
            char c = classContent.charAt(endIndex);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
            endIndex++;
        }

        if (braceCount != 0) {
            return "";
        }

        return classContent.substring(startIndex, endIndex + 1);
    }

    private static String removeEmptyLinesAndAnnotations(String classContent) {
        String classContentStr = removeAnnotations(classContent);
        classContentStr = classContentStr.replaceAll("(?m)^[ \t]*\r?\n", "");
        return classContentStr;
    }

    private static String extractMethodContent(String classContent, String methodSignature) {
        boolean isLambdaMethod = isLambdaMethod(methodSignature);

        String methodName = methodSignature.substring(0, methodSignature.indexOf('('));

        if(isLambdaMethod) {
            methodName = methodName.substring(methodName.indexOf('$') + 1);
            methodName = methodName.substring(0, methodName.indexOf('$'));
        }

        List<String> methods = extractMethods(classContent, methodName);

        for (String method : methods) {
            if(isLambdaMethod) {
                return method;
            }

            ArrayList<String> methodParameters = extractParametersTypes(method, methodName);
            ArrayList<String> expectedParameters = Arrays
                    .stream(methodSignature.substring(methodSignature.indexOf("(") + 1, methodSignature.indexOf(")"))
                    .split(","))
                    .collect(Collectors.toCollection(ArrayList::new));

            if(expectedParameters.size() == 1 && expectedParameters.get(0).equals("")) {
                expectedParameters.clear();
            }

            if (areParametersTypesEqual(methodParameters, expectedParameters)) {
                return method;
            }
        }

        return "";
    }

    private static List<String> extractMethods(String classContent, String methodName) {
        List<String> methods = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?s)" + methodName + "\\([^\\)]*\\)\\s*\\{([^\\}]*)\\}");
        Matcher matcher = pattern.matcher(classContent);
        while (matcher.find()) {
            methods.add(matcher.group(0));
        }
        return methods;
    }

    private static ArrayList<String> extractParametersTypes(String method, String methodName) {
        ArrayList<String> parametersTypes = new ArrayList<>();

        int startIndex = method.indexOf(methodName);
        int endIndex = method.indexOf(')', startIndex);
        if (startIndex == -1 || endIndex == -1) {
            return parametersTypes;
        }

        method = method.substring(startIndex + methodName.length() + 1, endIndex);

        String[] parameters = method.split(",");

        for (String parameter: parameters) {
            if (parameter.equals("")) continue;
            String[] parts = parameter.split(" ");

            if(parts.length < 2) continue;

            String type = parts[parts.length - 2];

            parametersTypes.add(type);
        }

        return parametersTypes;
    }

    private static String removeAnnotations(String content) {
        StringBuilder result = new StringBuilder();
        boolean isAnnotation = false;
        boolean skipNextCharacter = false;
        int annotationOpenCount = 0;
        int openCount = 0;

        for (char c : content.toCharArray()) {
            if(skipNextCharacter) {
                skipNextCharacter = false;
                continue;
            }
            if (c == '@' && !isAnnotation) {
                isAnnotation = true;
                annotationOpenCount = openCount;
            }
            else if(Character.isWhitespace(c) && isAnnotation && openCount == annotationOpenCount){
                isAnnotation = false;
            }

            if(!isAnnotation) result.append(c);

            if (c == '(') {
                openCount++;
            }
            else if (c == ')') {
                openCount--;
                if(openCount == annotationOpenCount) {
                    isAnnotation = false;
                    skipNextCharacter = true;
                }
            }
        }

        return result.toString();
    }

    private static boolean areParametersTypesEqual(ArrayList<String> methodParameters, ArrayList<String> expectedParameters) {
        if (methodParameters.size() != expectedParameters.size()) {
            return false;
        }

        for (int i = 0; i < methodParameters.size(); i++) {
            String methodParam = methodParameters.get(i);
            methodParam = methodParam.replaceAll("<.*>", "");
            String expectedParam = expectedParameters.get(i);

            if (isClassNameWithPackage(methodParam) && !methodParam.equals(expectedParam)) {
                return false;
            }
            else if (!isClassNameWithPackage(methodParam) && !getClassNameWithoutPackage(methodParam).equals(getClassNameWithoutPackage(expectedParam))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isClassNameWithPackage(String className) {
        return className.contains(".");
    }

    private static String getClassNameWithoutPackage(String fullClassName) {
        int lastDotIndex = fullClassName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return fullClassName.substring(lastDotIndex + 1);
        }
        return fullClassName;
    }

    private static boolean isLambdaMethod(String method) {
        return method.contains("lambda$");
    }

    private static int countLines(String text) {
        return text.split("\\r?\\n").length;
    }
}
