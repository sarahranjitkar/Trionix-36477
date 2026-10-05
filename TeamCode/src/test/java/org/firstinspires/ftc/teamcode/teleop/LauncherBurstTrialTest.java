package org.firstinspires.ftc.teamcode.teleop;

import org.junit.Test;
import static org.firstinspires.ftc.teamcode.teleop.LauncherBurstTrial.TARGET_RPM;
import static org.junit.Assert.*;

public class LauncherBurstTrialTest {
    @Test public void selectedTargetControlsReadinessAndMetrics() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, .5, true, 2800);
        tick(trial, 2400, .5);
        assertTrue(Double.isNaN(trial.settledSeconds));
        assertFalse(trial.gateOpen());
        while (!trial.feeding() && now < 3) tick(trial, 2800, .02);
        assertTrue(trial.feeding());
        tick(trial, 2600, .2);
        assertEquals(200, trial.feedRmsError(), 1e-9);
        assertEquals(2800, trial.targetRpm(), 0);
        LauncherBurstTrial next = new LauncherBurstTrial(now, .5, true, 2000);
        assertEquals(2000, next.targetRpm(), 0);
        assertEquals(2800, trial.targetRpm(), 0);
    }

    @Test public void targetBoundsAreValidated() {
        new LauncherBurstTrial(0, 2, false, 1500);
        new LauncherBurstTrial(0, 2, false, 3000);
        for (double bad : new double[]{1499, 3001, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new LauncherBurstTrial(0, 2, false, bad));
        }
    }

    private double now;
    private void tick(LauncherBurstTrial trial, double rpm, double duration) {
        int ticks = (int) Math.ceil(duration / 0.02);
        for (int i = 0; i < ticks; i++) { now += 0.02; trial.update(now, rpm); }
    }
    private void reachFeed(LauncherBurstTrial trial) {
        for (int i = 0; i < 150 && !trial.feeding(); i++) tick(trial, TARGET_RPM, 0.02);
        assertTrue(trial.feeding());
    }

    @Test public void crossingTargetDoesNotStartFeeding() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        tick(trial, TARGET_RPM, 0.2);
        tick(trial, 2100, 0.1);
        assertFalse(trial.gateOpen());
        assertTrue(Double.isNaN(trial.settledSeconds));
        reachFeed(trial);
        assertTrue(trial.settledSeconds >= 0.6);
    }

    @Test public void gateOpensBeforeIntakeAndBothStopBeforeRecoveryCompletes() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 0.5);
        while (!trial.gateOpen() && now < 2) tick(trial, TARGET_RPM, 0.02);
        assertEquals(LauncherBurstTrial.Phase.GATE_OPENING, trial.phase());
        assertFalse(trial.feeding());
        tick(trial, TARGET_RPM, 0.2);
        assertFalse(trial.feeding());
        reachFeed(trial);
        tick(trial, TARGET_RPM, 0.52);
        assertEquals(LauncherBurstTrial.Phase.RECOVERY, trial.phase());
        assertFalse(trial.gateOpen());
        assertFalse(trial.feeding());
        tick(trial, TARGET_RPM, 1.1);
        assertEquals(LauncherBurstTrial.Phase.DONE, trial.phase());
        assertTrue(trial.postFeedRecoverySeconds >= 0.3);
    }

    @Test public void spinOnlyNeverOpensGate() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2, false);
        for (int i = 0; i < 100; i++) {
            tick(trial, TARGET_RPM, 0.02);
            assertFalse(trial.gateOpen());
        }
        assertEquals(LauncherBurstTrial.Phase.DONE, trial.phase());
        assertTrue(Double.isNaN(trial.minFeedRpm));
    }

    @Test public void unreachableRpmTimesOutWithoutFeeding() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        tick(trial, 1500, 8.1);
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        assertFalse(trial.gateOpen());
        assertEquals(-1, trial.suggestedGain(new double[]{4.8, .32, .2, 5}));
    }

    @Test public void loadDipMeasuresErrorAndSettledRecovery() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        reachFeed(trial);
        tick(trial, 2000, 0.4);
        tick(trial, TARGET_RPM, 0.5);
        assertEquals(2000, trial.minFeedRpm, 0);
        assertEquals(1, trial.recoveredDips);
        assertTrue(trial.maxDipRecoverySeconds >= 0.65);
        assertTrue(trial.feedRmsError() > 150);
        assertTrue(trial.feedInBandPercent() < 70);
        tick(trial, TARGET_RPM, 3);
        assertEquals(LauncherBurstTrial.Phase.DONE, trial.phase());
        double[] gains = {4.8, .32, .2, 5};
        assertEquals(0, trial.suggestedGain(gains));
        assertEquals(5.28, trial.suggestedValue(gains), 1e-9);
    }

    @Test public void stallAbortsAndLeavesNoFeedingCommand() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        reachFeed(trial);
        tick(trial, 500, 0.3);
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        assertFalse(trial.gateOpen());
        assertFalse(trial.feeding());
        assertEquals(-1, trial.suggestedGain(new double[]{4.8, .32, .2, 5}));
    }

    @Test public void neverRecoveredDipIsNotReportedAsRecovered() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, .5);
        reachFeed(trial);
        tick(trial, 2000, 4.7);
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        assertTrue(trial.unrecoveredDip());
        assertEquals(0, trial.recoveredDips);
        assertTrue(Double.isNaN(trial.postFeedRecoverySeconds));
    }

    @Test public void slowSamplingAndInvalidEncoderAbort() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        trial.update(.3, TARGET_RPM);
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        LauncherBurstTrial invalid = new LauncherBurstTrial(0, 2);
        invalid.update(.02, Double.NaN);
        assertTrue(invalid.finished());
        assertFalse(invalid.gateOpen());
    }

    @Test public void baselineInstabilityDelaysFeedAndEventuallyTimesOut() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        tick(trial, TARGET_RPM, .5);
        for (int i = 0; i < 25; i++) { tick(trial, TARGET_RPM, .4); tick(trial, 2100, .1); }
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        assertFalse(trial.gateOpen());
    }

    @Test public void abortIsTerminalAndBadConfigurationRejected() {
        LauncherBurstTrial trial = new LauncherBurstTrial(0, 2);
        reachFeed(trial);
        trial.abort("Operator abort");
        tick(trial, TARGET_RPM, 2);
        assertEquals(LauncherBurstTrial.Phase.ABORTED, trial.phase());
        assertFalse(trial.feeding());
        assertThrows(IllegalArgumentException.class, () -> new LauncherBurstTrial(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LauncherBurstTrial(0, 7));
    }
}
