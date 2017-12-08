package latticesim;

import org.apache.commons.math3.random.MersenneTwister;

public class LatticeSimulation {
    private MersenneTwister random;

    /**
     *  Construct with random seed value (0 to not set a seed).
     *
     *  @param seed Random seed.  Pass 0 to not set a seed.
     */
    public LatticeSimulation(long seed) {
        if (seed == 0) {
            random = new MersenneTwister();
        } else {
            random = new MersenneTwister(seed);
        }
    }

    /**
     * Run without recording statistics, until simulated time reaches
     * tEnd.
     *
     * Assumes that there are no absorbing states (i.e. that
     * transitions.getExitRate() is always greater than zero).
     */
    public void run(
        LatticeConfiguration config, Transitions transitions, double tEnd
    ) {
        double t = 0;

        while (true) {
            double exitRate = transitions.getExitRate();

            double dt = -Math.log(random.nextDouble()) / exitRate;

            t += dt;

            if (t > tEnd) {
                break;
            }

            transitions.doTransition(random.nextDouble() * exitRate);
        }
    }

    /**
     * Run, recording statistics, until simulated time exceeds tEnd.
     */
    public void run(
        LatticeConfiguration config, Transitions transitions,
        double tEnd, DensityStatistics stats
    ) {
        double t = 0;

        while (true) {
            double exitRate = transitions.getExitRate();

            double dt = -Math.log(random.nextDouble()) / exitRate;

            if (t + dt >= tEnd) {
                stats.update(config, tEnd - t);

                return;
            }
            
            stats.update(config, dt);

            t += dt;

            transitions.doTransition(random.nextDouble() * exitRate, stats);
        }
    }

    /**
     * Run in verbose mode, without recording statistics,
     * until simulated time exceeds tEnd
     */
    public void runVerbose(
        LatticeConfiguration config, Transitions transitions, double tEnd
    ) {
        double t = 0;

        while (true) {
            double exitRate = transitions.getExitRate();

            double dt = -Math.log(random.nextDouble()) / exitRate;

            t += dt;

            if (t > tEnd) {
                break;
            }

            transitions.doTransition(random.nextDouble() * exitRate);

            System.out.println(config.toString() + " : " + t);
        }
    }

    /**
     * Run in verbose mode, recording statistics, until simulated time exceeds
     * tEnd.
     */
    public void runVerbose(
        LatticeConfiguration config, Transitions transitions, double tEnd,
        DensityStatistics stats
    ) {
        double t = 0;

        while (true) {
            double exitRate = transitions.getExitRate();

            double dt = -Math.log(random.nextDouble()) / exitRate;

            if (t + dt >= tEnd) {
                stats.update(config, tEnd - t);

                return;
            }
 
            stats.update(config, dt);

            t += dt;

            transitions.doTransition(random.nextDouble() * exitRate, stats);

            System.out.println(config.toString());
        }
    }

    /**
     * main() implementing very simple interface.
     *
     * All options are read from a JSON file given as stdin.  All output
     * is written to stdout as a JSON file.  In verbose mode,
     * step-by-step lattice configurations are also written to stdout.
     */
    public static void main(String [] args) throws Exception {
        SimOptions options = SimOptions.fromJSON(SimUtil.getStdIn());

        LatticeSimulation sim = new LatticeSimulation(options.seed);

        double tMax = options.tMax;

        double tSkip = options.tSkip;
        if (tSkip > tMax) {
            tSkip = tMax;
        }

        double t = 0;

        LatticeConfiguration config = new LatticeConfiguration(
            options.L,
            options.nstates,
            options.initialState
        );

        config.setMaxWindow(options.getMaxWindow());

        Transitions transitions = new Transitions(config, options.transitions);

        DensityStatistics stats
        =
        new DensityStatistics(options.L, options.nstates, options.transitions);

        if (options.verbose == 0) {
            // Don't print anything during the run

            // Don't record statistics for the first part
            sim.run(config, transitions, tSkip);

            // Now start recording stats
            sim.run(config, transitions, tMax - tSkip, stats);
        } else {
            // Verbose mode: print out configuration at each time increment

            System.out.println(config.toString());

            // Don't record statistics for the first part
            sim.runVerbose(config, transitions, tSkip);

            // Now start recording stats
            sim.runVerbose(config, transitions, tMax - tSkip, stats);
        }

        // Print out run options and results

        String outString =
            "{\"options\": "
            +
            options.toString()
            +
            ", \"results\": "
            +
            stats.summary()
            +
            ", \"endconfig\": "
            +
            config.toJson()
            +
            "}";

        System.out.println(outString);
    }
}