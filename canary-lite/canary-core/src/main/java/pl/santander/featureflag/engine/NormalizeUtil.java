package pl.santander.featureflag.engine;

import java.util.regex.Pattern;

public class NormalizeUtil {
    private static final Pattern PCT   = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
    private static final Pattern MS    = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ms");
    private static final Pattern NAMED = Pattern.compile("(?<=\\(|,)\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*:\\s*");

    public String normalize(String expr) {
        String s = expr;
        s = s.replace('\"', '\'');          // "..." -> '...'
        s = NAMED.matcher(s).replaceAll(""); // service: -> (usunięte)
        s = PCT.matcher(s).replaceAll("$1"); // 0.5% -> 0.5 (nasze UDF-y zwracają %)
        s = MS.matcher(s).replaceAll("$1");  // 300ms -> 300
        return s.trim();
    }
}
