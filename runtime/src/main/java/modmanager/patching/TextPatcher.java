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

public class TextPatcher {
    static Pattern charDetect = Pattern.compile("char_start\\s*dir\\s*\"(.*)\"\\s*file\\s*\"(.*)\"\\s*char_end", Pattern.MULTILINE);
    static Pattern charIndex = Pattern.compile("name_id[\\s=](\\d*)\\s*\\n", Pattern.DOTALL);
    static Pattern charCollect = Pattern.compile("collect\\s*\"(.*?)\"");
    public static int nextCharacter = 2009;

    public static void patchTextFile(String mod, String dst, String original, PatcherState state){
        switch (new File(mod).getName().toLowerCase(Locale.ROOT)){
            case "chars.txt" -> patchChars(mod, dst, state);
            case "collection.txt" -> patchCollections(mod, dst, original);
            case "english.txt" -> patchEnglish(mod, dst, original, state);
        }
    }

    private static void patchCollections(String mod, String dst, String original){
        try {
            var sourceData = FileUtils.readFileToString(new File(mod),  Charset.defaultCharset());
            var originalData = FileUtils.readFileToString(new File(original),  Charset.defaultCharset());

            var srcLines = sourceData.split("\n");
            var originalLines = originalData.split("\n");

            var originalLineSet = List.of(originalLines);

            var diff = new ArrayList<String>();
            for(var newLine : List.of(srcLines)){
                if(!originalLineSet.contains(newLine)){
                    diff.add(newLine);
                }
            }

            var newText = "\n\n" + String.join("\n", diff);
            Files.write(Paths.get(dst), newText.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void patchEnglish(String mod, String dst, String original, PatcherState state){
        try {
            var sourceData = FileUtils.readFileToString(new File(mod),  Charset.defaultCharset());
            var targetData = FileUtils.readFileToString(new File(dst),  Charset.defaultCharset());
            var originalData = FileUtils.readFileToString(new File(original),  Charset.defaultCharset());

            var newChars = tokenizeLang(sourceData);
            var targets = tokenizeLang(targetData);
            var originals = tokenizeLang(originalData);

            var differingChars = new HashMap<Integer, String>();

            for(var newChar : newChars.keySet()){
                if(!originals.containsKey(newChar) || !originals.get(newChar).equals(newChars.get(newChar)) || state.charIDChanges.containsKey(newChar)){
                    if(state.charIDChanges.containsKey(newChar)){
                        differingChars.put(state.charIDChanges.get(newChar), newChars.get(newChar));
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

            FileUtils.writeStringToFile(new File(dst), finalString, Charset.defaultCharset());
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
    private static void patchChars(String mod, String dst, PatcherState state){
        var originalChars = new ArrayList<DirFile>();
        var newChars = new ArrayList<DirFile>();

        try {
            var sourceData = FileUtils.readFileToString(new File(mod),  Charset.defaultCharset());
            var targetData = FileUtils.readFileToString(new File(dst),  Charset.defaultCharset());

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

            repairNewCharacterIndices(newChars, state.charIDChanges);
            copyLRModeCharacters(newChars);


            var strB = new StringBuilder();
            for(var mergedChar : merged){
                strB.append("char_start\n");
                strB.append("\tdir \"").append(mergedChar.dir).append("\"\n");
                strB.append("\tfile \"").append(mergedChar.file).append("\"\n");

                strB.append("char_end\n");
            }

            var finalString = strB.toString();

            FileUtils.writeStringToFile(new File(dst), finalString, Charset.defaultCharset());
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

    private static void repairNewCharacterIndices(List<DirFile> indices, Map<Integer, Integer> characterIDChanges){
        for(var character : indices){
            try {
                var file = ManagerProperties.PROPERTIES.getProperty("outputInstall") + "\\chars\\" + character.dir + "\\" + character.file + ".txt";
                var sourceData = FileUtils.readFileToString(new File(file),  Charset.defaultCharset());

                var matcher = charIndex.matcher(sourceData);
                if(matcher.find()){
                    var charIndex = Integer.valueOf(matcher.group(1));
                    sourceData = sourceData.replaceAll("(?s)name_id.*?\\n", "");

                    if(characterIDChanges.containsKey(charIndex)){
                        var newIdx = characterIDChanges.get(charIndex);
                        sourceData = "name_id=" + newIdx + "\n" + sourceData;
                    }else{
                        BottomPane.log(character.file + " " + nextCharacter);
                        characterIDChanges.put(charIndex, nextCharacter);
                        sourceData = "name_id=" + nextCharacter + "\n" + sourceData;
                        nextCharacter++;
                    }

                    FileUtils.writeStringToFile(new File(file),  sourceData, Charset.defaultCharset());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static record PatcherState(Map<Integer, Integer> charIDChanges){
        public PatcherState(){
            this(new HashMap<>());
        }
    }
}
