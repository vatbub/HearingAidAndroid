package com.github.vatbub.hearingaid;

import android.content.Context;
import android.support.annotation.RawRes;
import android.util.SparseArray;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import ru.noties.markwon.Markwon;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.renderer.SpannableRenderer;
import ru.noties.markwon.spans.SpannableTheme;

/**
 * Wrapping class to prerender markdown to html
 */

public class MarkdownRenderer {
    private static final Map<Context, MarkdownRenderer> instances = new HashMap<>();
    private final SparseArray<CharSequence> results;
    private final SparseArray<ResultStatus> resultStatusArray;
    private Parser parser;
    private SpannableConfiguration spannableConfiguration;
    private Context context;

    private MarkdownRenderer(Context context) {
        results = new SparseArray<>();
        resultStatusArray = new SparseArray<>();
        setContext(context);
    }

    public static MarkdownRenderer getInstance(Context context) {
        if (!instances.containsKey(context))
            instances.put(context, new MarkdownRenderer(context));

        return instances.get(context);
    }

    public void resetInstance(Context context) {
        instances.remove(context);
    }

    public CharSequence getCachedRenderResult(@RawRes int markdownFile) throws IOException {
        if (resultStatusArray.get(markdownFile, ResultStatus.NOT_STARTED) == ResultStatus.NOT_STARTED)
            return renderSynchronously(markdownFile);


        while (resultStatusArray.get(markdownFile, ResultStatus.NOT_STARTED) != ResultStatus.READY)
            System.out.println("Waiting for rendering to finish...");

        return results.get(markdownFile);
    }

    public void prerender(@RawRes final int markdownFile) {
        new Thread(() -> {
            try {
                renderSynchronously(markdownFile);
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Implement Bugsnag
            }
        }).start();
    }

    private void createSpannableConfiguration() {
        SpannableTheme theme = SpannableTheme.builderWithDefaults(getContext())
                .headingBreakHeight(0)
                .build();
        spannableConfiguration = SpannableConfiguration.builder(getContext())
                .theme(theme)
                .build();
    }

    public CharSequence renderSynchronously(@RawRes int markdownFile) throws IOException {
        resultStatusArray.put(markdownFile, ResultStatus.RENDERING);
        String lines = readLines(getContext().getResources().openRawResource(markdownFile));
        if (parser == null)
            parser = Markwon.createParser();

        if (spannableConfiguration == null)
            createSpannableConfiguration();

        SpannableRenderer spannableRenderer = new SpannableRenderer();
        Node node = parser.parse(lines);
        CharSequence res = spannableRenderer.render(spannableConfiguration, node);
        results.put(markdownFile, res);
        resultStatusArray.put(markdownFile, ResultStatus.READY);
        return res;
    }

    private String readLines(InputStream input) throws IOException {
        InputStreamReader reader = new InputStreamReader(input);
        return readLines(reader);
    }

    /**
     * Get the contents of a <code>Reader</code> as a list of Strings,
     * one entry per line.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     *
     * @param input the <code>Reader</code> to read from, not null
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    private String readLines(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        StringBuilder res = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            res.append(line).append("\n");
            line = reader.readLine();
        }
        return res.toString();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private enum ResultStatus {
        READY, RENDERING, NOT_STARTED
    }
}
