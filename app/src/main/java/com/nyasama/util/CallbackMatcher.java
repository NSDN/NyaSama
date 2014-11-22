package com.nyasama.util;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by oxyflour on 2014/11/22.
 * REF: http://stackoverflow.com/questions/375420/java-equivalent-to-phps-preg-replace-callback
 */
public class CallbackMatcher {
    public static interface Callback
    {
        public String foundMatch(MatchResult matchResult);
    }

    private final Pattern pattern;

    public CallbackMatcher(String regex, int style)
    {
        this.pattern = Pattern.compile(regex, style);
    }

    public String replaceMatches(String string, Callback callback)
    {
        final Matcher matcher = this.pattern.matcher(string);
        while(matcher.find())
        {
            final MatchResult matchResult = matcher.toMatchResult();
            final String replacement = callback.foundMatch(matchResult);
            string = string.substring(0, matchResult.start()) +
                    replacement + string.substring(matchResult.end());
            matcher.reset(string);
        }
        return string;
    }
}
