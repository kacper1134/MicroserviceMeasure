package fileReaders;

import utility.ClassTransformer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassFileReader {
    public static int getNumberOfLinesOfClass(String classContent, String className) {
        boolean isEnum = classContent.contains("enum " + className);
        boolean isException = isExceptionClass(classContent, className);
        boolean isNumberedClass = isNumberedClass(className);

        classContent = ClassTransformer.removeUnusedParts(className, classContent);
        if (isEnum || isNumberedClass) {
            return countLines(classContent) + 1;
        }
        if (isException) {
            return countLines(classContent) + 2;
        }
        return countLines(classContent);
    }

    public static int getNumberOfLinesOfMethod(String classContent, String className, String methodSignature) {
        boolean isEnum = classContent.contains("enum " + className);

        classContent = ClassTransformer.removeUnusedParts(className, classContent);
        if(isLambdaMethod(methodSignature)) {
            return 0;
        }

        String methodContent = extractMethodContent(classContent, methodSignature, isEnum);

        return countLines(methodContent);
    }

    private static String extractMethodContent(String classContent, String methodSignature, boolean isEnum) {
        String methodName = methodSignature.substring(0, methodSignature.indexOf('('));

        methodName = ClassTransformer.extractName(methodName);

        if (methodName.contains("$")) {
            methodName = methodName.substring(methodName.indexOf('$') + 1);
        }

        List<String> methods = extractMethods(classContent, methodName);

        for (String method : methods) {
            ArrayList<String> methodParameters = extractParametersTypes(method, methodName);
            ArrayList<String> expectedParameters = Arrays
                    .stream(methodSignature.substring(methodSignature.indexOf("(") + 1, methodSignature.indexOf(")"))
                    .split(","))
                    .collect(Collectors.toCollection(ArrayList::new));

            if(expectedParameters.size() == 1 && expectedParameters.get(0).equals("")) {
                expectedParameters.clear();
            }

            if (areParametersTypesEqual(methodParameters, expectedParameters, isEnum, classContent)) {
                return method;
            }
        }

        return "";
    }

    private static List<String> extractMethods(String classContent, String methodName) {
        List<String> extractedMethods = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("(public|protected|private|static|\\s)*[\\w\\<\\>\\[\\]]*\\s*" + methodName + "\\s*\\([^\\)]*\\)\\s*(throws\\s+[\\w\\.\\s,]+)?\\s*\\{");
        Pattern interfacePattern = Pattern.compile("([\\w\\<\\>\\[\\]]*(\\s|\\<|\\>)+\\s*" + methodName + "\\s*\\([^\\)]*\\);)");
        String[] lines = classContent.split("\n");

        boolean insideMethod = false;
        int openBracesCount = 0;
        StringBuilder currentMethod = new StringBuilder();

        for (String line : lines) {
            Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.find()) {
                insideMethod = true;
                openBracesCount = 1;
                currentMethod.append(line.trim()).append("\n");
            } else if (insideMethod) {
                currentMethod.append(line.trim()).append("\n");

                openBracesCount += line.chars().filter(ch -> ch == '{').count();
                openBracesCount -= line.chars().filter(ch -> ch == '}').count();

                if (openBracesCount <= 0) {
                    extractedMethods.add(currentMethod.toString());
                    currentMethod.setLength(0);
                    insideMethod = false;
                }
            }

            Matcher interfaceMatcher = interfacePattern.matcher(line);
            if (interfaceMatcher.find()) {
                extractedMethods.add(line.trim());
            }
        }

        extractedMethods.sort(Comparator.comparingInt(method -> -countLines(method)));

        return extractedMethods;
    }

    private static ArrayList<String> extractParametersTypes(String method, String methodName) {
        ArrayList<String> parametersTypes = new ArrayList<>();

        int startIndex = method.indexOf(methodName);
        int endIndex = method.indexOf(')', startIndex);
        if (startIndex == -1 || endIndex == -1) {
            return parametersTypes;
        }

        method = method.substring(startIndex + methodName.length() + 1, endIndex);

        method = ClassTransformer.removeAngleBrackets(method);

        String[] parameters = method.split(",");

        for (String parameter: parameters) {
            if (parameter.equals("")) continue;
            String[] parts = parameter.split(" ");

            if(parts.length < 2) continue;

            String type = parts[parts.length - 2];
            type = type.replace("...", "[]");

            parametersTypes.add(type);
        }

        return parametersTypes;
    }

    private static boolean areParametersTypesEqual(ArrayList<String> methodParameters, ArrayList<String> expectedParameters, boolean isEnum, String classContent) {
        if (isEnum) {
            int expectedParameterIndex = expectedParameters.size() - 1;

            for (int i = methodParameters.size() - 1; i >= 0; i--) {
                if(expectedParameterIndex < 0) {
                    return false;
                }
                String methodParam = methodParameters.get(i);
                methodParam = methodParam.replaceAll("<.*>", "");
                String expectedParam = expectedParameters.get(expectedParameterIndex--);

                methodParam = methodParam.trim();
                expectedParam = expectedParam.trim();

                String[] parts = expectedParam.split("\\$");

                if(parts.length > 1) {
                    expectedParam = parts[parts.length - 1];
                }

                if(isGenericType(classContent, methodParam)) {
                    methodParam = "Object";
                }

                if (isClassNameWithPackage(methodParam) && !methodParam.equals(expectedParam)) {
                    return false;
                }
                else if (!isClassNameWithPackage(methodParam) && !getClassNameWithoutPackage(methodParam).equals(getClassNameWithoutPackage(expectedParam))) {
                    return false;
                }
            }

            return true;
        }
        if (methodParameters.size() != expectedParameters.size()) {
            return false;
        }

        for (int i = 0; i < methodParameters.size(); i++) {
            String methodParam = methodParameters.get(i);
            methodParam = methodParam.replaceAll("<.*>", "");
            String expectedParam = expectedParameters.get(i);

            methodParam = methodParam.trim();
            expectedParam = expectedParam.trim();

            String[] parts = expectedParam.split("\\$");

            if(parts.length > 1) {
                expectedParam = parts[parts.length - 1];
            }

            if(isGenericType(classContent, methodParam)) {
                methodParam = "Object";
            }

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

    private static boolean isExceptionClass(String classContent, String className) {
        String pattern = className + "\\s*extends\\s+\\w*Exception";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(classContent);
        return matcher.find();
    }

    private static boolean isNumberedClass(String className) {
        Pattern pattern = Pattern.compile("\\$\\d+$");
        Matcher matcher = pattern.matcher(className);
        return matcher.find();
    }

    private static boolean isGenericType(String classContent, String typeName) {
        String pattern = "\\s+<" + Pattern.quote(typeName) + "\\s*(,\\s*\\w+)*>\\s+";

        Pattern regexPattern = Pattern.compile(pattern);

        Matcher matcher = regexPattern.matcher(classContent);
        return matcher.find();
    }

    private static int countLines(String text) {
        if(text.isEmpty()) return 0;
        return text.split("\\r?\\n").length;
    }
}
