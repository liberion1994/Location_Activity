package algorithms;

import models.Record;

import java.util.ArrayList;

/**
 * Created by apple on 16/3/13.
 */
public class CRFsRunner {

    public class Period {
        public double activityProbabilityVector[];
        public double trajectoryFeatureProbabilityVector[];

    }

    private ArrayList<Record> targetRecords;

    private double statusTransitionMatrix[][];

    /*
     * should consider last activity
     * but ignore the unimportant activity
     */
    private double significantActivityTransitionMatrix[][];

    private double durationRegressionVector[];

    public void initParams() {}

    public void initPeriods() {}

    public void startAlgorithm() {}

    /*
     * calculate one clique's conditional probability
     *
     * parameters:
     * unlabeled period
     * label to be examined
     *
     * pre activity
     * status transition matrix
     * trajectory feature probability matrix
     * activity probability vector
     * duration regression
     */
    public double calcCliqueProbability() {
        return 0;
    }

    /*
     * read settings(activity types & matrix)
     */
    public void loadSettings() {}

}
