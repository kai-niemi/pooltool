package io.cockroachdb.pool.configurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.util.StringUtils;

@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        DataJdbcRepositoriesAutoConfiguration.class,
})
public class Application {
    private static void printHelpAndExit(Consumer<AnsiConsole> message) {
        try (Terminal terminal = TerminalBuilder.terminal()) {
            AnsiConsole console = new AnsiConsole(terminal);
            console.green("Usage: java -jar pc.jar [options] [args...]").nl().nl();
            console.yellow("Options include:").nl();
            {
                console.cyan("--profiles [profile,..]   override spring profiles to activate").nl();
                console.cyan("--verbose                 enables the 'verbose' profile for extensive logging").nl();
                console.cyan("--help                    this help").nl();
            }
            console.nl();
            message.accept(console);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThroughArgs = new LinkedList<>();

        Set<String> profiles =
                StringUtils.commaDelimitedListToSet(System.getProperty("spring.profiles.active"));

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--help")) {
                printHelpAndExit(ansiConsole -> {
                });
            } else if (arg.equals("--verbose")) {
                profiles.add("verbose");
            } else if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printHelpAndExit(ansiConsole -> {
                        ansiConsole.red("Expected list of profile names");
                    });
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else {
                if (arg.startsWith("--") || arg.startsWith("@")) {
                    passThroughArgs.add(arg);
                } else {
                    printHelpAndExit(ansiConsole -> {
                        ansiConsole.red("Unknown argument: " + arg).nl().nl();
                    });
                }
            }
        }

        if (!profiles.isEmpty()) {
            System.setProperty("spring.profiles.active", String.join(",", profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.SERVLET)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[] {}));
    }
}
