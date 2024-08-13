package bytecode_manipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Chao Chen
 */
public class StatsHolder {
    private static final Map<String, List<Long>> DATA = new HashMap<>();

    public static void notifyStats(String className, String methodName, long startTime, long endTime) {
        String key = getKey(className, methodName);
        List<Long> stats = DATA.get(key);
        if (stats == null) {
            stats = new ArrayList<>();
            DATA.put(key, stats);
        }
        stats.add(endTime - startTime);
    }

    public static List<Long> getStats(String className, String methodName) {
        return DATA.get(getKey(className, methodName));
    }

    private static String getKey(String className, String methodName) {
        return className+"_"+methodName;
    }

}
