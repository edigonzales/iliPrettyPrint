package ch.so.agi.pprint;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "iliprettyprint", 
        description = "A tool to pretty print INTERLIS model files.", 
        mixinStandardHelpOptions = false

        )
public class App implements Callable<Integer> {

    @Option(names = "--ili", description = "Path to the input ILI file", required = true)
    private Path iliFile;

    @Option(names = "--out", description = "Path to the output directory", paramLabel = "<outputDirectory>", required = false)
    private Path outputDir;

    @Option(names = "--modeldir", description = "List of directories/repositories", paramLabel = "<modeldir>", required = false)
    private String modeldir;
    
    @Override
    public Integer call() {
        System.out.println("ILI file: " + iliFile.toAbsolutePath());
        System.out.println("Output file: " + outputDir.toAbsolutePath());
        System.out.println("Model dir: " + modeldir);
        
        if (outputDir == null) {
            outputDir = iliFile.getParent();
        }
        boolean ret = PrettyPrint.run(new File[] {iliFile.toFile()}, outputDir, modeldir);
        return ret ? 0 : 1;        
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}


