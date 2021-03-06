package sql.lang.ast.table;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.datatype.Value;
import util.Pair;
import sql.lang.datatype.ValType;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/18/15.
 * Join is implemented as cartesian product
 */
public class JoinNode extends TableNode {

    List<TableNode> tableNodes = new ArrayList<TableNode>();

    public JoinNode(List<TableNode> tableNodes) {
        this.tableNodes = tableNodes;
    }

    @Override
    public TableNode pruneColumns(List<String> neededColumns, boolean isTopLevel) {
        return new JoinNode(tableNodes.stream()
                .map((t) -> t.pruneColumns(neededColumns, false))
                .collect(Collectors.toList()));
    }

    private boolean isSelectStar(TableNode t) {
        if (t instanceof SelectNode) {
            SelectNode s = (SelectNode) t;
            return (s.getTableNode() instanceof NamedTable && s.getFilter() instanceof EmptyFilter && s.getSchema().equals(s.getTableNode().getSchema()));
        }

        return false;
    }

    @Override
    public Map<String, String> eliminateRenames(boolean isTopLevel) {
        Map<String, String> mapping = new HashMap<>();

        for (int i = 0; i < tableNodes.size(); i++) {
            if (tableNodes.get(i) instanceof RenameTableNode) {
                RenameTableNode rename = (RenameTableNode) tableNodes.get(i);

                if (isSelectStar(rename.getTableNode())) {
                    mapping.putAll(SelectNode.buildRenameMap(rename));

                    tableNodes.set(i, rename.getTableNode());
                }
            }
        }

        return mapping;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        List<Table> tables = new ArrayList<Table>();
        for (TableNode tn : tableNodes) {
            tables.add(tn.eval(env));
        }
        Table currentTable = tables.get(0);
        for (int i = 1; i < tables.size(); i ++) {
            currentTable = Table.joinTwo(currentTable, tables.get(i));
        }
        return currentTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        for (TableNode tn : this.tableNodes) {
            schema.addAll(tn.getSchema());
        }
        return schema;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<ValType> valTypes = new ArrayList<ValType>();
        for (TableNode tn : this.tableNodes) {
            valTypes.addAll(tn.getSchemaType());
        }
        return valTypes;
    }

    @Override
    // the level of join is the maximum table level + 1
    public int getNestedQueryLevel() {
        int lv = 0;
        for (TableNode tn : this.tableNodes) {
            if (tn.getNestedQueryLevel() > lv)
                lv = tn.getNestedQueryLevel();
        }
        return lv + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = this.tableNodes.get(0).prettyPrint(1, true).trim();
        for (int i = 1; i < this.tableNodes.size(); i ++) {
            result += " Join \r\n" + this.tableNodes.get(i).prettyPrint(1,true);
        }
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.tableNodes.forEach(tn -> result.addAll(tn.getAllHoles()));
        return result;
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new JoinNode(
            this.tableNodes.stream()
                .map(t -> t.instantiate(env)).collect(Collectors.toList()));
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return new JoinNode(
                this.tableNodes.stream()
                        .map(t -> t.substNamedVal(vnsb)).collect(Collectors.toList()));    }

    @Override
    public List<NamedTable> namedTableInvolved() {
        List<NamedTable> result = new ArrayList<>();
        for (TableNode t : this.tableNodes) {
            result.addAll(t.namedTableInvolved());
        }
        return result;
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        return new JoinNode(
                tableNodes.stream()
                        .map(tn -> tn.tableSubst(pairs))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<String> originalColumnName() {
        List<String> result = new ArrayList<>();
        for (TableNode tn : this.tableNodes) {
            result.addAll(tn.originalColumnName());
        }
        return result;
    }

    public List<TableNode> getTableNodes() { return this.tableNodes; }

    @Override
    public double estimateAllFilterCost() {
        return tableNodes.stream().map(tn -> tn.estimateAllFilterCost()).reduce(0., (x,y) -> (x + y));
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> result = new ArrayList<>();
        for (TableNode tn : tableNodes) {
            result.addAll(tn.getAllConstants());
        }

        return result;
    }

    @Override
    public String getQuerySkeleton() {
        return "(J" + tableNodes.stream().map(tn -> tn.getQuerySkeleton()).reduce("", (x,y)-> (x + " " + y)) + ")";
    }

    public List<Table> getNamedTableInJoin() {
        List<Table> result = new ArrayList<>();
        for (TableNode tn : this.tableNodes) {
            if (tn instanceof NamedTable)
                result.add(((NamedTable) tn).getTable());
            if (tn instanceof JoinNode) {
                result.addAll(((JoinNode) tn).getNamedTableInJoin());
            }
        }
        return result;
    }

}
