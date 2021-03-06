package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.ast.table.TableNode;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

import java.util.*;

/**
 * Created by clwang on 1/10/16.
 * TODO: not yet implemented
 */
public class FilterHole implements Filter, Hole {
    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return false;
    }

    @Override
    public int getFilterLength() {
        return 0;
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public String prettyPrint(int indentLv) {
        return null;
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return Arrays.asList(this);
    }

    @Override
    public List<Value> getAllConstants() {
        return new ArrayList<>();
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return this;
    }

    public SortedSet<Integer> filter(Table t) { return new TreeSet<>(); } // undefined behavior

    @Override
    public List<String> getColumnNames() {
        return null;
    }

    @Override
    public boolean applyRename(Map<String, String> rename) {
        return false;
    }

    @Override
    public Filter colToNestedQ(String colName, TableNode nested) {
        return this;
    }

}
