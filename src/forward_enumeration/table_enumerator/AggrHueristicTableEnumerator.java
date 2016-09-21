package forward_enumeration.table_enumerator;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.EnumSelectTableNode;
import forward_enumeration.context.QueryChest;
import forward_enumeration.table_enumerator.hueristics.TableNaturalJoinWithAggr;
import sql.lang.ast.table.TableNode;
import util.RenameTNWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This enumerator features precomputing aggregation before enumerating other things
 * Created by clwang on 3/21/16.
 */
public class AggrHueristicTableEnumerator extends  AbstractTableEnumerator {
    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {

        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        List<TableNode> agrTables = TableNaturalJoinWithAggr.naturalJoinWithAggregation(ec);
        qc.insertQueries(agrTables.stream()
                        .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList()));

        for (int i = 0; i < depth; i ++) {
            ec.setTableNodes(qc.getRepresentativeTableNodes());
            List<TableNode> tableNodes = EnumSelectTableNode.enumSelectNode(ec);
            qc.insertQueries(tableNodes);
            /*tableNodes = EnumJoinTableNodes.enumJoinWithFilter(ec);
            ec = EnumContext.extendTable(ec,
                    tableNodes.stream()
                            .map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList())); */
        }
        return qc;
    }
}