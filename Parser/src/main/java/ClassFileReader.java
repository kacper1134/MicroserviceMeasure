import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassFileReader {
    public static int getNumberOfLinesOfClass(String classContent, String className) {
        className = extractClassName(className);
        classContent = extractClassContent(classContent, className);
        classContent = removeEmptyLinesAndAnnotations(classContent);
        classContent = removeAngleBrackets(classContent);
        classContent = deleteStaticBlocks(classContent);
        classContent = removeInnerClasses(classContent);
        classContent = removeDefaultMethod(classContent);
        classContent = removeEmptyLinesAndAnnotations(classContent);

        return countLines(classContent);
    }

    public static int getNumberOfLinesOfMethod(String classContent, String className, String methodSignature) {
        className = extractClassName(className);
        classContent = extractClassContent(classContent, className);
        classContent = removeEmptyLinesAndAnnotations(classContent);
        classContent = removeAngleBrackets(classContent);
        classContent = deleteStaticBlocks(classContent);
        classContent = removeInnerClasses(classContent);
        classContent = removeDefaultMethod(classContent);
        classContent = removeEmptyLinesAndAnnotations(classContent);

        if(isLambdaMethod(methodSignature)) {
            return 0;
        }

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

    private static String extractClassContent(String classContent, String className) {
        String regex = "\\b(class|interface|enum|record)\\s+" + className;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(classContent);

        int startIndex = -1;

        if (matcher.find()) {
            startIndex = matcher.start();
        }

        if (startIndex == -1) {
            return "";
        }

        int openingBraceIndex = classContent.indexOf("{", startIndex);
        if (openingBraceIndex == -1) {
            return "";
        }

        int endIndex = openingBraceIndex;
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

        return classContent.substring(openingBraceIndex + 1, endIndex).trim();
    }

    private static String removeEmptyLinesAndAnnotations(String classContent) {
        String classContentStr = removeAnnotations(classContent);
        classContentStr = classContentStr.replaceAll("(?m)^[ \t]*\r?\n", "").replaceAll("\\s+$", "");
        return classContentStr;
    }

    private static String extractMethodContent(String classContent, String methodSignature) {
        String methodName = methodSignature.substring(0, methodSignature.indexOf('('));

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

            if (areParametersTypesEqual(methodParameters, expectedParameters)) {
                return method;
            }
        }

        return "";
    }

    private static List<String> extractMethods(String classContent, String methodName) {
        List<String> extractedMethods = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]*\\s*" + methodName + "\\s*\\([^\\)]*\\)\\s*(throws\\s+[\\w\\.\\s,]+)?\\s*\\{?");
        Pattern interfacePattern = Pattern.compile("([\\w\\<\\>\\[\\]]+\\s+" + methodName + "\\s*\\([^\\)]*\\);)");
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
                if(openCount == annotationOpenCount && isAnnotation) {
                    isAnnotation = false;
                    skipNextCharacter = true;
                }
            }
        }

        return result.toString();
    }

    private static String removeAngleBrackets(String input) {
        String regex = "<[^>]*>";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(input);

        return matcher.replaceAll("");
    }

    private static String removeInnerClasses(String classContent) {
        Pattern pattern = Pattern.compile("(?:(?:public|private|sealed|static|abstract|final)\\s+)*\\s*(?:class|enum|interface|record)\\s+\\w+\\b");
        Matcher matcher = pattern.matcher(classContent);

        while (matcher.find()) {
            int indexOfOfStartOfClass = classContent.indexOf(matcher.group().trim());

            int indexOfEndOfClass = indexOfOfStartOfClass;

            int braceCount = 0;

            while (indexOfEndOfClass < classContent.length()) {
                char c = classContent.charAt(indexOfEndOfClass);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        break;
                    }
                }
                indexOfEndOfClass++;
            }

            if (indexOfEndOfClass == classContent.length()) {
                classContent = classContent.substring(0, indexOfOfStartOfClass);
            } else {
                classContent = classContent.substring(0, indexOfOfStartOfClass) + classContent.substring(indexOfEndOfClass + 1);
            }

            pattern = Pattern.compile("(?:(?:public|private|sealed|static|abstract|final)\\s+)*\\s*(?:class|enum|interface|record)\\s+\\w+\\b");
            matcher = pattern.matcher(classContent);
        }

        return classContent;
    }

    private static String removeDefaultMethod(String input) {
        String regex = "\\bdefault\\b.*?;";
        String result = input.replaceAll(regex, ";");
        result = result.replaceAll("\\)\\s*;", ");");

        return result;
    }

    private static boolean areParametersTypesEqual(ArrayList<String> methodParameters, ArrayList<String> expectedParameters) {
        if (methodParameters.size() != expectedParameters.size()) {
            return false;
        }

        for (int i = 0; i < methodParameters.size(); i++) {
            String methodParam = methodParameters.get(i);
            methodParam = methodParam.replaceAll("<.*>", "");
            String expectedParam = expectedParameters.get(i);

            methodParam = methodParam.trim();
            expectedParam = expectedParam.trim();

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

    private static String deleteStaticBlocks(String classContent) {
        return classContent.replaceAll("(?s)static\\s*\\{.*?\\}", "");
    }

    private static int countLines(String text) {
        if(text.isEmpty()) return 0;
        return text.split("\\r?\\n").length;
    }

    public static void main(String [] args) {
        String classContent = """
                package com.macro.mall.demo.validator;
                                
                import java.lang.annotation.Documented;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                import javax.validation.Constraint;
                import javax.validation.Payload;
                                
                @Documented
                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.FIELD, ElementType.PARAMETER})
                @Constraint(
                    validatedBy = {FlagValidatorClass.class}
                )
                public @interface FlagValidator {
                    String[] value() default {};
                                
                    String message() default "flag is not found";
                                
                    Class<?>[] groups() default {};
                                
                    Class<? extends Payload>[] payload() default {};
                }
                """;
        System.out.println(removeDefaultMethod(classContent));
        System.out.println(extractMethods(removeDefaultMethod(classContent), "value"));
    }
}
