package modmanager.patching;

import modmanager.ManagerProperties;
import modmanager.ui.BottomPane;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextPatcher {
    static Pattern charDetect = Pattern.compile("char_start\\s*dir\\s*\"(.*)\"\\s*file\\s*\"(.*)\"\\s*char_end", Pattern.MULTILINE);
    static Pattern charIndex = Pattern.compile("name_id[\\s=](\\d*)\\s*\\n", Pattern.DOTALL);
    static Pattern charCollect = Pattern.compile("collect\\s*\"(.*?)\"");
    public static Map<Integer, Integer> characterIDReplacements = new HashMap<>();
    public static int nextCharacter = 2009;

    public static void patchTextFile(String source, String target, String original){
        switch (new File(source).getName().toLowerCase(Locale.ROOT)){
            case "chars.txt" -> patchChars(source, target);
            case "collection.txt" -> patchCollections(source, target, original);
            case "english.txt" -> patchEnglish(source, target, original);
        }
    }

    private static void patchCollections(String source, String target, String original){
        try {
            var sourceData = FileUtils.readFileToString(new File(source),  Charset.defaultCharset());
            var originalData = FileUtils.readFileToString(new File(original),  Charset.defaultCharset());

            var srcLines = sourceData.split("\n");
            var srcLineSet = new ArrayList<String>();
            srcLineSet.addAll(List.of(srcLines));

            var originalLines = originalData.split("\n");
            var originalLineSet = new ArrayList<String>();
            originalLineSet.addAll(List.of(originalLines));

            var diff = new ArrayList<String>();
            for(var newLine : srcLineSet){
                if(!originalLineSet.contains(newLine)){
                    //var diffMatcher = charCollect.matcher(newLine);
                    //if(diffMatcher.find()){
                    //    diff.add("collect \"" + diffMatcher.group(1) + "\"");
                   // }
                    diff.add(newLine);
                }
            }

            var newText = "\n\n" + diff.stream().collect(Collectors.joining("\n"));
            Files.write(Paths.get(target), newText.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void patchEnglish(String source, String target, String original){
        try {
            var sourceData = FileUtils.readFileToString(new File(source),  Charset.defaultCharset());
            var targetData = FileUtils.readFileToString(new File(target),  Charset.defaultCharset());
            var originalData = FileUtils.readFileToString(new File(original),  Charset.defaultCharset());


            var newChars = tokenizeLang(sourceData);
            var targets = tokenizeLang(targetData);
            var originals = tokenizeLang(originalData);

            var differingChars = new HashMap<Integer, String>();

            for(var newChar : newChars.keySet()){
                if(!originals.containsKey(newChar) || !originals.get(newChar).equals(newChars.get(newChar)) || characterIDReplacements.containsKey(newChar)){
                    if(characterIDReplacements.containsKey(newChar)){
                        differingChars.put(characterIDReplacements.get(newChar), newChars.get(newChar));
                    }else{
                        differingChars.put(newChar, newChars.get(newChar));

                    }
                }
            }

            targets.putAll(differingChars);

            var strB = new StringBuilder();
            for(var entry : targets.entrySet()){
                strB.append(entry.getKey()).append(" \"").append(entry.getValue()).append("\"\n");
            }
            var finalString = strB.toString();

            FileUtils.writeStringToFile(new File(target), finalString, Charset.defaultCharset());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, String> tokenizeLang(String input){
        var lines = input.split("\n");

        var langLines = new LinkedHashMap<Integer, String>();

        for(var line : lines){
            line = line.trim();
            if(line.contains(";")) continue;
            if(line.trim().isEmpty() || line.startsWith(";") || line.startsWith("//")) continue;
            if(!line.contains("\"")){
                langLines.put(Integer.valueOf(line.trim()), "");
                continue;
            }

            var index = line.trim().substring(0, line.trim().indexOf("\"")).replace("pc", "").trim();
            var str = line.substring(line.trim().indexOf("\"")).replace("\"", "").trim();

            langLines.put(Integer.valueOf(index), str);
        }

        return langLines;
    }

    static record DirFile(String dir, String file){}
    private static void patchChars(String source, String target){
        characterIDReplacements.clear();

        var originalChars = new ArrayList<DirFile>();
        var newChars = new ArrayList<DirFile>();

        try {
            var sourceData = FileUtils.readFileToString(new File(source),  Charset.defaultCharset());
            var targetData = FileUtils.readFileToString(new File(target),  Charset.defaultCharset());

            var sourceMatch = charDetect.matcher(sourceData);
            while (sourceMatch.find()) {
                newChars.add(new DirFile(sourceMatch.group(1), sourceMatch.group(2)));
            }

            var targetMatch = charDetect.matcher(targetData);
            while (targetMatch.find()) {
                originalChars.add(new DirFile(targetMatch.group(1), targetMatch.group(2)));
            }

            var merged = new LinkedHashSet<DirFile>();
            merged.addAll(originalChars);
            merged.addAll(newChars);
            newChars.removeAll(originalChars);
            repairNewCharacterIndices(newChars);
            copyLRModeCharacters(newChars);


            var strB = new StringBuilder();
            for(var mergedChar : merged){
                strB.append("char_start\n");
                strB.append("\tdir \"").append(mergedChar.dir).append("\"\n");
                strB.append("\tfile \"").append(mergedChar.file).append("\"\n");

                strB.append("char_end\n");
            }

            var finalString = strB.toString();

            FileUtils.writeStringToFile(new File(target), finalString, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyLRModeCharacters(List<DirFile> indices){
        for(var character : indices){
            try {
                if(new File(ManagerProperties.PROPERTIES.getProperty("outputInstall") + "\\chars\\" + character.dir + "\\" + character.file + "_LR_PC.GHG").exists()) continue;
                Files.copy(Path.of(ManagerProperties.PROPERTIES.getProperty("outputInstall") + "\\chars\\" + character.dir + "\\" + character.file + "_PC.GHG"),
                        Path.of(ManagerProperties.PROPERTIES.getProperty("outputInstall") + "\\chars\\" + character.dir + "\\" + character.file + "_LR_PC.GHG"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void repairNewCharacterIndices(List<DirFile> indices){
        for(var character : indices){
            try {
                var file = ManagerProperties.PROPERTIES.getProperty("outputInstall") + "\\chars\\" + character.dir + "\\" + character.file + ".txt";
                var sourceData = FileUtils.readFileToString(new File(file),  Charset.defaultCharset());

                var matcher = charIndex.matcher(sourceData);
                if(matcher.find()){
                    var charIndex = Integer.valueOf(matcher.group(1));
                    sourceData = sourceData.replaceAll("(?s)name_id.*?\\n", "");
                    if(characterIDReplacements.containsKey(charIndex)){
                        var newIdx = characterIDReplacements.get(charIndex);
                        sourceData = "name_id=" + newIdx + "\n" + sourceData;
                        FileUtils.writeStringToFile(new File(file),  sourceData, Charset.defaultCharset());
                    }else{
                        var newIdx = nextCharacter;
                        BottomPane.log(character.file + " " + newIdx);
                        nextCharacter++;
                        characterIDReplacements.put(charIndex, newIdx);
                        sourceData = "name_id=" + newIdx + "\n" + sourceData;
                        FileUtils.writeStringToFile(new File(file),  sourceData, Charset.defaultCharset());
                    }

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
