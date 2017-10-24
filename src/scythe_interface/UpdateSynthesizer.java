package scythe_interface;

import forward_enumeration.table_enumerator.AbstractTableEnumerator;
import global.GlobalConfig;
import sql.lang.AbstractSetClause;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UpdateSynthesizer {
    public static long TimeOut = 600000;

    public static void SynthesizeUpdate(String exampleFilePath, AbstractTableEnumerator enumerator) {
        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(exampleFilePath);
        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Update synthesis start]]");
            System.out.println("\tFile: " + exampleFilePath);
            System.out.println("\tEnumerator: " + enumerator.getClass().getSimpleName());
        }

        if (!validateUpdateInput(exampleDS)) {
            throw new IllegalStateException("Example file contained illegal update input");
        }

        Table output = exampleDS.output;
        Table update = exampleDS.tUpdate;

        // Can safely assume at this point that update table and output have same number of rows
        int nRows = output.getContent().size();
        List<Integer> updatedIdxs = new ArrayList<>();

        for (int i = 0; i < nRows; i++) {
            if (!output.getContent().get(i).equals(update.getContent().get(i))) {
                updatedIdxs.add(i);
            }
        }

        List<Filter> candidateFilters = new ArrayList<>();
        List<TableRow> updatedRows = IntStream.range(0, nRows)
                .filter(updatedIdxs::contains)
                .mapToObj(update.getContent()::get)
                .collect(Collectors.toList());

        Table updatedOnly = new Table();
        updatedOnly.initialize(update.getName(), update.getSchema(), updatedRows);

        if (updatedIdxs.size() != nRows) {

            exampleDS.output = updatedOnly;
            List<TableNode> candidates =
                    Synthesizer.SynthesizeWAggr(exampleFilePath, enumerator, 0, exampleDS);

            for (TableNode t : candidates) {
                if (! (t instanceof SelectNode)) {
                    System.err.println("Something went wrong, synthesized classifier " + t);
                    continue;
                }

                // Cast is safe based on check above
                SelectNode s = (SelectNode) t;
                candidateFilters.add(s.getFilter());
            }
        } else {
            candidateFilters.add(new EmptyFilter());
        }

        List<TableRow> updatedOuputs = IntStream.range(0, nRows)
                .filter(updatedIdxs::contains)
                .mapToObj(output.getContent()::get)
                .collect(Collectors.toList());

        Table updatedOutputOnly = new Table();
        updatedOutputOnly.initialize(output.getName(), output.getSchema(), updatedOuputs);

        AbstractSetClause setClause = AbstractSetClause.enumerateFromIO(
                updatedOnly,
                updatedOutputOnly,
                exampleDS,
                (ds -> Synthesizer.SynthesizeWAggr(exampleFilePath, enumerator, -1, ds)));

        System.out.println("UPDATE " + update.getName());
        System.out.println("SET " + setClause.concretize());

        String whereFilter = candidateFilters.get(0).prettyPrint(0);
        if (whereFilter.length() > 0) {
            System.out.println("WHERE " + candidateFilters.get(0).prettyPrint(0));
        }
    }

    private static boolean validateUpdateInput(ExampleDS exampleDS) {
        Table output = exampleDS.output;
        Table update = exampleDS.tUpdate;

        if (output.getContent().size() != update.getContent().size()) {
            return false;
        }

        if (output.getSchema().size() != update.getSchema().size()) {
            return false;
        }

        return true;
    }
}
