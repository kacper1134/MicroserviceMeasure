package utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTransformer {
    public static String removeUnusedParts(String className, String classContent) {
        className = extractName(className);
        classContent = removeBracketsInsideStringLiteral(classContent);
        classContent = extractClassContent(classContent, className);
        classContent = removeAnnotations(classContent);
        classContent = removeEmptyLines(classContent);
        classContent = removeStaticBlocks(classContent);
        classContent = removeInnerClasses(classContent);
        classContent = removeDefaultMethod(classContent);
        classContent = removeEmptyLines(classContent);

        return classContent;
    }

    public static String extractName(String name) {
        Pattern pattern = Pattern.compile("\\$\\d+$");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            name = name.substring(0, matcher.start());
        }

        int lastIndex = name.lastIndexOf('$');
        if (lastIndex != -1 && lastIndex < name.length() - 1) {
            return name.substring(lastIndex + 1);
        }
        return name;
    }

    public static String removeAngleBrackets(String input) {
        StringBuilder result = new StringBuilder();
        int angleBracketCount = 0;
        int startIndex = -1;

        for (char c : input.toCharArray()) {
            if (c == '<') {
                angleBracketCount++;
                if (angleBracketCount == 1) {
                    startIndex = result.length();
                }
            } else if (c == '>') {
                angleBracketCount--;
                if (angleBracketCount == 0) {
                    result.delete(startIndex, result.length());
                }
            }
            else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static String removeBracketsInsideStringLiteral(String input) {
        StringBuilder result = new StringBuilder();
        boolean isStringLiteral = false;
        char stringLiteralOpenChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!isStringLiteral && (c == '\"' || c == '\'')) {
                stringLiteralOpenChar = c;
                isStringLiteral = true;
            }
            else if (isStringLiteral && c == stringLiteralOpenChar && input.charAt(i - 1) != '\\') {
                isStringLiteral = false;
            }


            if(isStringLiteral && (c == '{' || c == '}' || c == '<' || c == '>')) continue;

            result.append(c);
        }

        return result.toString();
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

    private static String removeEmptyLines(String classContent) {
        return classContent.replaceAll("(?m)^[ \t]*\r?\n", "").replaceAll("\\s+$", "");
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

    private static String removeStaticBlocks(String classContent) {
        String pattern = "static\\s*\\{.*?\n\\s*}";

        return classContent.replaceAll("(?s)" + pattern, "");
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
}
