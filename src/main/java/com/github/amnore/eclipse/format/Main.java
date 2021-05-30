package com.github.amnore.eclipse.format;

import java.io.*;
import java.util.Properties;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class Main {
    private static CodeFormatter formatter;

    private static final Options options = new Options();

    static {
        options.addOption("c", "config", true, "path to config file");
        options.addOption("h", "help", false, "print this help message");
    }

    private static Properties getDefaultConfig() {
        var config = new Properties();
        config.setProperty("org.eclipse.jdt.core.formatter.join_wrapped_lines", "false");
        config.setProperty("org.eclipse.jdt.core.formatter.tabulation.char", "space");
        config.setProperty("org.eclipse.jdt.core.formatter.tabulation.size", "4");
        return config;
    }

    private static String formatDocument(int kind, String contents) {
        IDocument doc = new Document(contents);

        // do formatting
        TextEdit edit = formatter.format(kind, contents, 0, contents.length(), 0, null);
        if (edit == null)
            return contents;

        try {
            edit.apply(doc);
        } catch (BadLocationException e) {
            return contents;
        }
        return doc.get();
    }

    private static void formatStdin() throws IOException {
        var contents = new String(System.in.readAllBytes());
        contents = formatDocument(CodeFormatter.K_UNKNOWN | CodeFormatter.F_INCLUDE_COMMENTS, contents);
        System.out.write(contents.getBytes());
    }

    /**
     * Format the given Java source file.
     */
    private static void formatFile(File file) throws IOException {
        // read the file
        var in = new BufferedInputStream(new FileInputStream(file));
        var contents = new String(in.readAllBytes());
        in.close();

        // format the file (the meat and potatoes)
        int kind = (file.getName().equals(IModule.MODULE_INFO_JAVA)
                ? CodeFormatter.K_MODULE_INFO
                : CodeFormatter.K_COMPILATION_UNIT)
                | CodeFormatter.F_INCLUDE_COMMENTS;
        contents = formatDocument(kind, contents);

        // write the file
        @Cleanup
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(contents);
    }

    private static File[] processCommandLine(String[] argsArray) throws ParseException {
        var parser = new DefaultParser();
        var cmd = parser.parse(options, argsArray);

        if (cmd.hasOption("help")) {
            throw new PrintHelpMessageException();
        }

        Properties config = null;
        if (cmd.hasOption("config")) {
            var path = cmd.getOptionValue("config");
            config = readConfig(new File(path));

            if (config == null)
                throw new ConfigErrorException(path);
        } else {
            config = getDefaultConfig();
        }
        formatter = ToolFactory.createCodeFormatter(config, ToolFactory.M_FORMAT_EXISTING);

        // the remaining args are files to format
        var files = cmd.getArgList();
        if (files.isEmpty())
            return null;

        return files.stream()
                .map(File::new)
                .toArray(File[]::new);
    }

    /**
     * Return a Java Properties file representing the options that are in the
     * specified configuration file.
     */
    private static Properties readConfig(File configFile) {
        try {
            @Cleanup
            var stream = new BufferedInputStream(new FileInputStream(configFile));
            final Properties formatterOptions = new Properties();

            formatterOptions.load(stream);
            return formatterOptions;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            File[] filesToFormat = processCommandLine(args);

            if (filesToFormat == null) {
                formatStdin();
            } else {
                for (var file : filesToFormat) {
                    formatFile(file);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static class PrintHelpMessageException extends RuntimeException {
        @Override
        public String getMessage() {
            var buf = new ByteArrayOutputStream();
            var writer = new PrintWriter(buf);
            new HelpFormatter().printHelp(writer,
                    HelpFormatter.DEFAULT_WIDTH,
                    "eclipse-format [options] [<file> ...]",
                    "Standalone Eclipse Formatter",
                    options,
                    HelpFormatter.DEFAULT_LEFT_PAD,
                    HelpFormatter.DEFAULT_DESC_PAD,
                    "");
            writer.flush();
            return buf.toString();
        }
    }

    @AllArgsConstructor
    private static class ConfigErrorException extends RuntimeException {
        private final String cfgpath;

        @Override
        public String getMessage() {
            return String.format("Error reading config file %s", cfgpath);
        }
    }
}