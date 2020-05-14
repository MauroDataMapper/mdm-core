package uk.ac.ox.softeng.maurodatamapper.search;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.util.regex.Pattern;

/**
 * @since 28/03/2018
 */
public class PathTokenizerAnalyzer extends StopwordAnalyzerBase {
    /**
     * An unmodifiable set containing some common English words that are usually not
     * useful for searching.
     */
    public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param stopWords stop words
     */
    public PathTokenizerAnalyzer(CharArraySet stopWords) {
        super(stopWords);
    }

    /**
     * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
     */
    public PathTokenizerAnalyzer() {
        this(STOP_WORDS_SET);
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {

        Pattern pattern = Pattern.compile("/");
        Tokenizer source = new PatternTokenizer(pattern, -1);
        TokenStream filter = new StandardFilter(source);
        filter = new LowerCaseFilter(filter);
        filter = new StopFilter(filter, stopwords);
        return new TokenStreamComponents(source, filter);
    }
}
