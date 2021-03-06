package scythe_interface;

import forward_enumeration.context.EnumConfig;
import sql.lang.Table;
import sql.lang.ast.val.ConstantVal;
import util.TableInstanceParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The data structure used to store an example, with concrete enumeration context
 */
public class ExampleDS {
    public List<Table> inputs = new ArrayList<>();
    public Table tModify = null;
    public Table output;
    public EnumConfig enumConfig;

    private ExampleDS() {}

    public ExampleDS(ExampleDS example) {
        this.inputs = new ArrayList<>();
        for (Table t: example.inputs) {
            this.inputs.add(t.duplicate());
        }
        this.tModify = example.tModify.duplicate();
        this.output = example.output.duplicate();
        this.enumConfig = example.enumConfig;
    }

    public static ExampleDS readFromFile(String path) {

        ExampleDS example = new ExampleDS();

        List<String> fileContent;
        try {
            fileContent = Files.readAllLines(Paths.get(path)).stream().filter(t -> !t.startsWith("//")).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Set<String> usedInputTableNames = new HashSet<>();

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
                if (segName.startsWith("input")) {
                    String baseTableName = segName.substring("input".length());

                    boolean isTUpdate = false;
                    if (baseTableName.startsWith("*")) {
                        if (example.tModify != null) {
                            throw new IllegalStateException("Multiple update tables in input");
                        }

                        isTUpdate = true;
                        baseTableName = baseTableName.substring(1);
                    }

                    if (baseTableName.equals(""))
                        baseTableName = "input";
                    else
                        baseTableName = baseTableName.substring(1);

                    String tableName = baseTableName;
                    int id = 0;
                    while (usedInputTableNames.contains(tableName)) {
                        tableName = baseTableName + id;
                        id ++;
                    }
                    usedInputTableNames.add(tableName);

                    Table t = TableInstanceParser.tryParseTable(tableName, segContent);

                    if (isTUpdate) {
                        example.tModify = t;
                    }
                    example.inputs.add(t);
                } else if (segName.startsWith("output")) {
                    String baseTableName = segName.substring("output".length());
                    if (baseTableName.equals(""))
                        baseTableName = "output";
                    else
                        baseTableName = baseTableName.substring(1);

                    String tableName = baseTableName;
                    example.output = TableInstanceParser.tryParseTable(tableName, segContent);
                } else if (segName.startsWith("constraint")) {
                    example.enumConfig = new EnumConfig(segContent.stream().reduce(String::concat).get());
                } else if (segName.startsWith("solution")) {
                    //System.out.println("Stack Overflow Solution: " + segContent);
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