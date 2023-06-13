package psym.commandline;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import psym.runtime.scheduler.choiceorchestration.ChoiceLearningRewardMode;
import psym.runtime.scheduler.choiceorchestration.ChoiceLearningStateMode;
import psym.runtime.scheduler.choiceorchestration.ChoiceOrchestrationMode;
import psym.runtime.scheduler.choiceorchestration.ChoiceOrchestratorEpsilonGreedy;
import psym.runtime.scheduler.taskorchestration.TaskOrchestrationMode;
import psym.runtime.scheduler.taskorchestration.TaskOrchestratorCoverageEpsilonGreedy;
import psym.utils.GlobalData;
import psym.utils.StateHashingMode;
import psym.valuesummary.solvers.SolverType;
import psym.valuesummary.solvers.sat.expr.ExprLibType;

import java.io.*;
import java.util.Iterator;

import static java.lang.System.exit;

/**
 * Represents the commandline options for the tool
 */
public class PSymOptions {

    private static final Options options;
    private static HelpFormatter formatter = new HelpFormatter();
    private static final PrintWriter writer = new PrintWriter(System.out);

    static {
        options = new Options();

        // Basic options

        // strategy of exploration
        Option strategy = Option.builder("s")
                .longOpt("strategy")
                .desc("Exploration strategy: random, dfs, learn, symex (default: learn)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Strategy (string)")
                .build();
        options.addOption(strategy);

        // test driver name
        Option testName = Option.builder("tc")
                .longOpt("testcase")
                .desc("Test case to explore")
                .numberOfArgs(1)
                .hasArg()
                .argName("Test Case (string)")
                .build();
        options.addOption(testName);

        // time limit
        Option timeLimit = Option.builder("t")
                .longOpt("timeout")
                .desc("Timeout in seconds (disabled by default)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Time Limit (seconds)")
                .build();
        options.addOption(timeLimit);

        // memory limit
        Option memLimit = Option.builder("m")
                .longOpt("memout")
                .desc("Memory limit in Giga bytes (auto-detect by default)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Memory Limit (GB)")
                .build();
        options.addOption(memLimit);

        // output folder
        Option outputDir = Option.builder("o")
                .longOpt("outdir")
                .desc("Dump output to directory (absolute or relative path)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Output Dir (string)")
                .build();
        options.addOption(outputDir);

        // set the level of verbosity
        Option verbosity = Option.builder("v")
                .longOpt("verbose")
                .desc("Level of verbose log output during exploration (default: 0)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Log Verbosity (integer)")
                .build();
        options.addOption(verbosity);


        // Systematic exploration options

        // max number of executions for the search
        Option maxExecutions = Option.builder("i")
                .longOpt("iterations")
                .desc("Number of schedules to explore (default: 1)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Iterations (integer)")
                .build();
        options.addOption(maxExecutions);

        // max steps/depth bound for the search
        Option maxSteps = Option.builder("ms")
                .longOpt("max-steps")
                .desc("Max scheduling steps to be explored (default: 10,000)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Max Steps (integer)")
                .build();
        options.addOption(maxSteps);

        // whether or not to fail on reaching max step bound
        Option failOnMaxSteps = Option.builder("fms")
                .longOpt("fail-on-maxsteps")
                .desc("Consider it a bug if the test hits the specified max-steps")
                .numberOfArgs(0)
                .build();
        options.addOption(failOnMaxSteps);


        // Search prioritization options

        // whether or not to disable state caching
        Option stateHashing = Option.builder("sh")
                .longOpt("state-hashing")
                .desc("State hashing mode: none, exact, fast (default: exact)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Hashing Mode (string)")
                .build();
        options.addOption(stateHashing);

        // mode of choice orchestration
        Option choiceOrch = Option.builder("corch")
                .longOpt("choice-orch")
                .desc("Choice orchestration options: random, learn (default: learn)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Choice Orch. (string)")
                .build();
        options.addOption(choiceOrch);

        // mode of task orchestration
        Option taskOrch = Option.builder("torch")
                .longOpt("task-orch")
                .desc("Task orchestration options: astar, random, dfs, learn (default: learn)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Task Orch. (string)")
                .build();
        options.addOption(taskOrch);

        // max scheduling choice bound for the search
        Option maxSchedBound = Option.builder("sb")
                .longOpt("sched-bound")
                .desc("Max scheduling choice bound at each step during the search (default: 1)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Schedule Bound (integer)")
                .build();
        options.addOption(maxSchedBound);

        // max data choice bound for the search
        Option dataChoiceBound = Option.builder("db")
                .longOpt("data-bound")
                .desc("Max data choice bound at each step during the search (default: 1)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Data Bound (integer)")
                .build();
        options.addOption(dataChoiceBound);


        // Replay and debug options

        // read replayer state from file
        Option readReplayerFromFile = Option.builder("r")
                .longOpt("replay")
                .desc("Schedule file to replay")
                .numberOfArgs(1)
                .hasArg()
                .argName("File Name (string)")
                .build();
        options.addOption(readReplayerFromFile);


        // Advanced options

        // random seed for the search
        Option randomSeed = Option.builder()
                .longOpt("seed")
                .desc("Specify the random value generator seed")
                .numberOfArgs(1)
                .hasArg()
                .argName("Random Seed (integer)")
                .build();
        options.addOption(randomSeed);

        // whether or not to enable symmetry
        Option symmetry = Option.builder()
                .longOpt("use-symmetry")
                .desc("Enable symmetry-aware exploration")
                .numberOfArgs(0)
                .build();
        options.addOption(symmetry);

        // whether or not to disable stateful backtracking
        Option backtrack = Option.builder()
                .longOpt("no-backtrack")
                .desc("Disable stateful backtracking")
                .numberOfArgs(0)
                .build();
        options.addOption(backtrack);

        // max number of backtrack tasks per execution
        Option maxBacktrackTasksPerExecution = Option.builder()
                .longOpt("backtracks-per-iteration")
                .desc("Max number of backtracks to generate per iteration (default: 2)")
                .numberOfArgs(1)
                .hasArg()
                .argName("(integer)")
                .build();
        options.addOption(maxBacktrackTasksPerExecution);

        // mode of choice learning state mode
        Option choiceLearnState = Option.builder()
                .longOpt("learn-state")
                .desc("Learning state options: none, last, states, events, full (default: last)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Learn State (string)")
                .build();
        options.addOption(choiceLearnState);

        // mode of choice learning reward mode
        Option choiceLearnReward = Option.builder()
                .longOpt("learn-reward")
                .desc("Learning reward options: coverage, fixed (default: coverage)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Learn Reward (string)")
                .build();
        options.addOption(choiceLearnReward);

        // epsilon-greedy decay rate
        Option epsilonDecay = Option.builder()
                .longOpt("learn-decay")
                .desc("Decay rate for epsilon-greedy")
                .numberOfArgs(1)
                .hasArg()
                .argName("Decay Rate (double)")
                .build();
        options.addOption(epsilonDecay);

        // solver type
        Option solverType = Option.builder()
                .longOpt("solver")
                .desc("Solver type to use: bdd, yices2, monosat, z3, cvc5 (default: bdd)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Solver Type (string)")
                .build();
        options.addOption(solverType);

        // expression type
        Option exprLibType = Option.builder()
                .longOpt("expr")
                .desc("Expression type to use: bdd, fraig, aig, native (default: bdd)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Expression Type (string)")
                .build();
        options.addOption(exprLibType);

        // whether or not to enable filter-based reductions
        Option filters = Option.builder()
                .longOpt("use-filters")
                .desc("Enable filter-based reductions")
                .numberOfArgs(0)
                .build();
        options.addOption(filters);

        // read program state from file
        Option readFromFile = Option.builder()
                .longOpt("read")
                .desc("Name of the file with the program state")
                .numberOfArgs(1)
                .hasArg()
                .argName("File Name (string)")
                .build();
        options.addOption(readFromFile);

        // Enable writing the program state to file
        Option writeToFile = Option.builder()
                .longOpt("write")
                .desc("Enable writing program state")
                .numberOfArgs(0)
                .build();
        options.addOption(writeToFile);

        // whether or not to collect search stats
        Option collectStats = Option.builder()
                .longOpt("stats")
                .desc("Level of stats collection/reporting during the search (default: 1)")
                .numberOfArgs(1)
                .hasArg()
                .argName("Collection Level (integer)")
                .build();
        options.addOption(collectStats);


        // psym configuration file
        Option configFile = Option.builder()
                .longOpt("config")
                .desc("Name of the JSON configuration file")
                .numberOfArgs(1)
                .hasArg()
                .argName("File Name (string)")
                .build();
        options.addOption(configFile);

        // project name
        Option projName = Option.builder()
                .longOpt("projname")
                .desc("Project name")
                .numberOfArgs(1)
                .hasArg()
                .argName("Project Name (string)")
                .build();
        options.addOption(projName);


        // Help menu
        Option help = Option.builder("h")
                .longOpt("help")
                .desc("Show this help menu")
                .build();
        options.addOption(help);
    }

    private static void optionError(Option opt, String msg) {
        writer.println(msg);
        if (opt != null) {
            Options opts = new Options();
            opts.addOption(opt);
            formatter.printHelp(writer, 100, opt.getLongOpt(), "", opts, 2, 2, "Try --help for details.");
        }
        writer.flush();
        exit(10);
    }

    public static PSymConfiguration ParseCommandlineArgs(String[] args) {
        // Parse the commandline arguments
        CommandLineParser parser = new DefaultParser();
        formatter.setOptionComparator(null);
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printUsage(writer, 100, "java -jar <.jar-file>", options);
            writer.flush();
            System.out.println("Try --help for details.");
            exit(10);
        }

        PSymConfiguration config = new PSymConfiguration();

        if (cmd.getOptionValue("config") == null) {
            // Populate the configuration based on psym-config file (if exists)
            File tempFile = new File("psym-config.json");
            if (tempFile.exists()) {
                readConfigFile(config, tempFile.getAbsolutePath(), null);
            }
        }

        // Populate the configuration based on the commandline arguments
        for (Option option : cmd.getOptions()) {
            switch (option.getLongOpt()) {
                // basic options
                case "s":
                case "strategy":
                    switch (option.getValue()) {
                        case "random":
                            config.setToRandom();
                            break;
                        case "dfs":
                            config.setToDfs();
                            break;
                        case "learn-backtrack":
                            config.setToBacktrackLearn();
                            break;
                        case "learn-choice":
                            config.setToChoiceLearn();
                            break;
                        case "learn":
                        case "learn-all":
                            config.setToAllLearn();
                            break;
                        case "bmc":
                        case "sym":
                        case "symex":
                        case "symbolic":
                            config.setToSymex();
                            break;
                        case "fuzz":
                            config.setToFuzz();
                            break;
                        case "coverage":
                            config.setToCoverage();
                            break;
                        case "debug":
                            config.setToDebug();
                            break;
                        default:
                            optionError(option, String.format("Unrecognized strategy of exploration, got %s", option.getValue()));
                    }
                    break;
                case "tc":
                case "testcase":
                    config.setTestDriver(option.getValue());
                    break;
                case "t":
                case "timeout":
                    try {
                        config.setTimeLimit(Double.parseDouble(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected a double value, got %s", option.getValue()));
                    }
                    break;
                case "m":
                case "memout":
                    try {
                        config.setMemLimit(Double.parseDouble(option.getValue())*1024);
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected a double value, got %s", option.getValue()));
                    }
                    break;
                case "o":
                case "outdir":
                    config.setOutputFolder(option.getValue());
                    break;
                case "v":
                case "verbose":
                    try {
                        config.setVerbosity(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                // exploration options
                case "i":
                case "iterations":
                    try {
                        config.setMaxExecutions(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "ms":
                case "max-steps":
                    try {
                        config.setMaxStepBound(Integer.parseInt(option.getValue()) + 1);
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "fms":
                case "fail-on-maxsteps":
                    config.setFailOnMaxStepBound(true);
                    break;
                // search options
                case "sh":
                case "state-hashing":
                    switch (option.getValue()) {
                        case "none":
                            config.setStateHashingMode(StateHashingMode.None);
                            break;
                        case "exact":
                            config.setStateHashingMode(StateHashingMode.Exact);
                            break;
                        case "fast":
                            config.setStateHashingMode(StateHashingMode.Fast);
                            break;
                        default:
                            optionError(option, String.format("Unrecognized state hashing mode, got %s", option.getValue()));
                    }
                    break;
                case "corch":
                case "choice-orch":
                    switch (option.getValue()) {
                        case "none":
                            config.setChoiceOrchestration(ChoiceOrchestrationMode.None);
                            break;
                        case "random":
                            config.setChoiceOrchestration(ChoiceOrchestrationMode.Random);
                            break;
                        case "learn-ql":
                            config.setChoiceOrchestration(ChoiceOrchestrationMode.QLearning);
                            break;
                        case "learn":
                        case "learn-eg":
                            config.setChoiceOrchestration(ChoiceOrchestrationMode.EpsilonGreedy);
                            break;
                        default:
                            optionError(option, String.format("Unrecognized choice orchestration mode, got %s", option.getValue()));
                    }
                    break;
                case "learn-mode":
                case "learn-state":
                    switch (option.getValue()) {
                        case "none":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.None);
                            break;
                        case "depth":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.SchedulerDepth);
                            break;
                        case "last":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.LastStep);
                            break;
                        case "states":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.MachineState);
                            break;
                        case "states+last":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.MachineStateAndLastStep);
                            break;
                        case "events":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.MachineStateAndEvents);
                            break;
                        case "full":
                            config.setChoiceLearningStateMode(ChoiceLearningStateMode.FullState);
                            break;
                        default:
                            optionError(option, String.format("Unrecognized choice learning state mode, got %s", option.getValue()));
                    }
                    break;
                case "learn-reward":
                    switch (option.getValue()) {
                        case "none":
                            config.setChoiceLearningRewardMode(ChoiceLearningRewardMode.None);
                            break;
                        case "fixed":
                            config.setChoiceLearningRewardMode(ChoiceLearningRewardMode.Fixed);
                            break;
                        case "coverage":
                            config.setChoiceLearningRewardMode(ChoiceLearningRewardMode.Coverage);
                            break;
                        default:
                            optionError(option, String.format("Unrecognized choice learning reward mode, got %s", option.getValue()));
                    }
                    break;
                case "learn-decay":
                    try {
                        ChoiceOrchestratorEpsilonGreedy.setEPSILON_DECAY_FACTOR(Double.parseDouble(option.getValue()));
                        TaskOrchestratorCoverageEpsilonGreedy.setEPSILON_DECAY_FACTOR(Double.parseDouble(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected a double value, got %s", option.getValue()));
                    }
                    break;
                case "torch":
                case "task-orch":
                    switch (option.getValue()) {
                        case "dfs":
                            config.setTaskOrchestration(TaskOrchestrationMode.DepthFirst);
                            break;
                        case "random":
                            config.setTaskOrchestration(TaskOrchestrationMode.Random);
                            break;
                        case "astar":
                            config.setTaskOrchestration(TaskOrchestrationMode.CoverageAStar);
                            break;
                        case "learn":
                        case "learn-eg":
                            config.setTaskOrchestration(TaskOrchestrationMode.CoverageEpsilonGreedy);
                            break;
                        default:
                            optionError(option, String.format("Unrecognized task orchestration mode, got %s", option.getValue()));
                    }
                    break;
                case "sb":
                case "sched-bound":
                    try {
                        config.setSchedChoiceBound(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "db":
                case "data-bound":
                    try {
                        config.setDataChoiceBound(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                // replay options
                case "r":
                case "replay":
                    config.setReadReplayerFromFile(option.getValue());
                    File file = new File(config.getReadReplayerFromFile());
                    try {
                        file.getCanonicalPath();
                    } catch (IOException e) {
                        optionError(option, String.format("File %s does not exist", config.getReadReplayerFromFile()));
                    }
                    break;
                // advanced options
                case "seed":
                    try {
                        config.setRandomSeed(Long.parseLong(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "use-symmetry":
                    config.setUseSymmetry(true);
                    break;
                case "no-backtrack":
                    config.setUseBacktrack(false);
                    break;
                case "backtracks-per-iteration":
                    try {
                        config.setMaxBacktrackTasksPerExecution(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "solver":
                    switch (option.getValue()) {
                        case "abc":
                            config.setSolverType(SolverType.ABC);
                            break;
                        case "bdd":
                            config.setSolverType(SolverType.BDD);
                            break;
                        case "cbdd":
                            config.setSolverType(SolverType.CBDD);
                            break;
                        case "cvc5":
                            config.setSolverType(SolverType.CVC5);
                            break;
                        case "yices2":
                            config.setSolverType(SolverType.YICES2);
                            break;
                        case "z3":
                            config.setSolverType(SolverType.Z3);
                            break;
                        case "monosat":
                            config.setSolverType(SolverType.MONOSAT);
                            break;
//                        case "boolector":
//                            config.setSolverType(SolverType.JAVASMT_BOOLECTOR);
//                            break;
//                        case "mathsat5":
//                            config.setSolverType(SolverType.JAVASMT_MATHSAT5);
//                            break;
//                        case "princess":
//                            config.setSolverType(SolverType.JAVASMT_PRINCESS);
//                            break;
//                        case "smtinterpol":
//                            config.setSolverType(SolverType.JAVASMT_SMTINTERPOL);
//                            break;
                        default:
                            optionError(option, String.format("Expected a solver type, got %s", option.getValue()));
                    }
                    break;
                case "expr":
                    switch (option.getValue()) {
                        case "aig":
                            config.setExprLibType(ExprLibType.Aig);
                            break;
                        case "auto":
                            config.setExprLibType(ExprLibType.Auto);
                            break;
                        case "bdd":
                            config.setExprLibType(ExprLibType.Bdd);
                            break;
                        case "fraig":
                            config.setExprLibType(ExprLibType.Fraig);
                            break;
                        case "iaig":
                            config.setExprLibType(ExprLibType.Iaig);
                            break;
                        case "native":
                            config.setExprLibType(ExprLibType.NativeExpr);
                            break;
                        default:
                            optionError(option, String.format("Expected an expression type, got %s", option.getValue()));
                    }
                    break;
                case "use-filters":
                    config.setUseFilters(true);
                    break;
                case "read":
                    config.setReadFromFile(option.getValue());
                    File replayFile = new File(config.getReadFromFile());
                    try {
                        replayFile.getCanonicalPath();
                    } catch (IOException e) {
                        optionError(option, String.format("File %s does not exist", config.getReadFromFile()));
                    }
                    break;
                case "write":
                    config.setWriteToFile(true);
                    break;
                case "stats":
                    try {
                        config.setCollectStats(Integer.parseInt(option.getValue()));
                    } catch (NumberFormatException ex) {
                        optionError(option, String.format("Expected an integer value, got %s", option.getValue()));
                    }
                    break;
                case "config":
                    readConfigFile(config, option.getValue(), option);
                    break;
                case "projname":
                    config.setProjectName(option.getValue());
                    break;
                case "h":
                case "help":
                    formatter.printHelp(
                            100,
                            "java -jar <.jar-file> [options]",
                            "----------------------------\nCommandline options for PSym\n----------------------------",
                            options,
                            "See https://p-org.github.io/P/ for details.");
                    exit(0);
                    break;
                default:
                    optionError(option, String.format("Unrecognized option %s", option));
            }
        }

        // post process
        if (!config.isChoiceOrchestrationLearning()) {
            config.setChoiceLearningRewardMode(ChoiceLearningRewardMode.None);
        }
        return config;
    }

    public static void readConfigFile(PSymConfiguration config, String configFileName, Option option) {
        config.setConfigFile(configFileName);
        File configFile = new File(config.getConfigFile());
        try {
            configFile.getCanonicalPath();
            ParseConfigFile(config, configFile);
        } catch (IOException e) {
            optionError(option, String.format("File %s does not exist", config.getConfigFile()));
        }
    }

    private static void ParseConfigFile(PSymConfiguration config, File configFile) throws FileNotFoundException {
        InputStream configStream = new FileInputStream(configFile);
        assert(configStream != null);
        JSONTokener jsonTokener = new JSONTokener(configStream);
        JSONObject jsonObject = new JSONObject(jsonTokener);

        Iterator<String> keys = jsonObject.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            if (jsonObject.get(key) instanceof JSONObject) {
                JSONObject value = (JSONObject) jsonObject.get(key);
                switch (key) {
                    case "sync-events":
                        JSONArray syncEvents = value.getJSONArray("default");
                        for (int i = 0; i < syncEvents.length(); i++) {
                            String syncEventName = syncEvents.getString(i);
                            GlobalData.getSyncEvents().add(syncEventName);
                        }
                        break;
                    case "symmetric-machines":
                        JSONArray symMachineTypes = value.getJSONArray("default");
                        for (int i = 0; i < symMachineTypes.length(); i++) {
                            String symTypeName = symMachineTypes.getString(i);
                            GlobalData.getInstance().getSymmetryTracker().addSymmetryType(symTypeName);
                        }
                        break;
                    default:
                        optionError(null, String.format("Unrecognized key %s in config file", key));
                }
            }
        }
    }
}
