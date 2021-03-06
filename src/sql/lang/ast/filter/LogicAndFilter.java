package sql.lang.ast.filter;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.ast.table.TableNode;
import sql.lang.datatype.Value;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.Map;

/**
 * Created by clwang on 12/20/15.
 */
public class LogicAndFilter implements Filter {

    Filter f1;
    Filter f2;

    public LogicAndFilter(Filter f1, Filter f2) {
        this.f1 = f1;
        this.f2 = f2;
    }

    @Override
    public boolean filter(Environment env) throws SQLEvalException {
        return f1.filter(env) && f2.filter(env);
    }

    @Override
    public int getFilterLength() { return f1.getFilterLength() + f2.getFilterLength(); }

    @Override
    public int getNestedQueryLevel() {
        return f1.getNestedQueryLevel() > f2.getNestedQueryLevel() ?
                f1.getNestedQueryLevel() : f2.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        if (f1 instanceof EmptyFilter)
            return IndentionManagement.addIndention(f2.prettyPrint(0), indentLv);
        else if (f2 instanceof EmptyFilter)
            return IndentionManagement.addIndention(f1.prettyPrint(0), indentLv);
        else {
            String result = f1.prettyPrint(0) + "\r\n And " + f2.prettyPrint(0);
            return IndentionManagement.addIndention(result, indentLv);
        }
    }

    @Override
    public boolean containsExclusiveFilter(Filter f) {
        return f1.containsExclusiveFilter(f) && f2.containsExclusiveFilter(f);
    }

    public static Filter connectByAnd(List<Filter> filters) {
        Filter last = filters.get(0);
        for (int i = 1; i < filters.size(); i ++) {
            last = new LogicAndFilter(last, filters.get(i));
        }
        return last;
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = f1.getAllHoles();
        result.addAll(f2.getAllHoles());
        return result;
    }

    @Override
    public List<Value> getAllConstants() {
        List<Value> list =  f1.getAllConstants();
        list.addAll(f2.getAllConstants());
        return list;
    }

    @Override
    public Filter instantiate(InstantiateEnv env) {
        return new LogicAndFilter(f1.instantiate(env), f2.instantiate(env));
    }

    @Override
    public Filter substNamedVal(ValNodeSubstBinding vnsb) {
        return new LogicAndFilter(f1.substNamedVal(vnsb), f2.substNamedVal(vnsb));
    }

    @Override
    public List<String> getColumnNames() {
        List<String> names = f1.getColumnNames();
        List<String> names2 = f2.getColumnNames();
        names.addAll(names2);
        return names;
    }

    @Override
    public boolean applyRename(Map<String, String> rename) {
        return f1.applyRename(rename) || f2.applyRename(rename);
    }

    @Override
    public Filter colToNestedQ(String colName, TableNode nested) {
        return new LogicAndFilter(f1.colToNestedQ(colName, nested), f2.colToNestedQ(colName, nested));
    }

    public List<Filter> getAllFilters() {
        List<Filter> result = new ArrayList<>();
        if (f1 instanceof LogicAndFilter) {
            result.addAll(((LogicAndFilter) f1).getAllFilters());
        } else {
            result.add(f1);
        }

        if (f2 instanceof LogicAndFilter) {
            result.addAll(((LogicAndFilter) f2).getAllFilters());
        } else {
            result.add(f2);
        }

        return result;
    }

    public SortedSet<Integer> filter(Table t) {
        SortedSet<Integer> s1 = this.f1.filter(t);
        SortedSet<Integer> s2 = this.f2.filter(t);
        s1.retainAll(s2); // intersection
        return s1;
    }
}
