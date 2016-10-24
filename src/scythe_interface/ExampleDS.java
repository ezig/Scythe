package scythe_interface;

import forward_enumeration.context.EnumConfig;
import sql.lang.Table;
import util.TableInstanceParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The data structure used to store an example, with concrete enumeration context
 */
public class ExampleDS {
    public List<Table> inputs = new ArrayList<>();
    public Table output;
    public EnumConfig enumConfig;

    private ExampleDS() {}
    public static ExampleDS readFromFile(String path) {

        ExampleDS example = new ExampleDS();

        List<String> fileContent = new ArrayList<>();
        try {
            fileContent = Files.readAllLines(Paths.get(path)).stream().filter(t -> !t.startsWith("//")).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int i = 0;
        while (i < fileContent.size()) {

            if (fileContent.get(i).startsWith("#")) {
                String segName = fileContent.get(i).substring(1).trim();
                i ++;
                List<String> segContent = new ArrayList<>();
                while (i < fileContent.size() && !fileContent.get(i).startsWith("#")) {
                    if (!fileContent.get(i).trim().isEmpty())
                        segContent.add(fileContent.get(i));
                    i ++;
                }
                if (segName.equals("input")) {
                    example.inputs.add(TableInstanceParser.tryParseTable("input" + (example.inputs.size() + 1), segContent));
                } else if (segName.equals("output")) {
                    example.output = TableInstanceParser.tryParseTable("output", segContent);
                } else if (segName.equals("constraint")) {
                    example.enumConfig = new EnumConfig(segContent.stream().reduce(String::concat).get());
                }
            } else {
                i ++;
            }
        }

        return example;
    }

    @Override
    public String toString() {
        String s = "";
        for (Table t : inputs) {
            s += "----\n" + t.toString() + "\n";
        }
        s += "----\n" + output.toString() + "\n";
        s += enumConfig.toString();
        return s;
    }

}