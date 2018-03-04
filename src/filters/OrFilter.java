package filters;

import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

public class OrFilter implements Filter {
    private final Filter left;
    private final Filter right;


    public OrFilter(Filter left, Filter right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean matches(Status s) {
        return left.matches(s) || right.matches(s);
    }

    @Override
    public List<String> terms() {
        List<String> result = new ArrayList<>();
        result.addAll(left.terms());
        result.addAll(right.terms());
        return result;
    }

    @Override
    public String toString() {
        return "(" + left + " or " + right + ")";
    }
}
