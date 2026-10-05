package org.firstinspires.ftc.teamcode.teleop;

/** Hardware-independent experiment timing and measurements. All times are monotonic seconds. */
public final class LauncherBurstTrial {
    // Default selection; every trial captures its own immutable target.
    public static final double TARGET_RPM = 2400;
    public static final double MIN_TARGET_RPM = 1500, MAX_TARGET_RPM = 3000;
    private final double targetRpm;
    public static final double TOLERANCE_RPM = 75;
    public static final double SETTLE_SECONDS = 0.30;
    public enum Phase { SPINUP, BASELINE, GATE_OPENING, FEED, RECOVERY, DONE, ABORTED }

    private Phase phase = Phase.SPINUP;
    private final double start, feedSeconds;
    private final boolean burst;
    private double phaseStart, lastTime, bandStart = Double.NaN, lowStart = Double.NaN;
    private double dipStart = Double.NaN, dipBandStart = Double.NaN;
    private double baselineSum, baselineMin = Double.POSITIVE_INFINITY, baselineMax;
    private double feedSquaredError, feedErrorArea, feedObservedSeconds, feedGoodSeconds;
    private int baselineSamples;
    public double firstBandSeconds = Double.NaN, settledSeconds = Double.NaN;
    public double peakSpinupRpm, minFeedRpm = Double.NaN, maxDipRecoverySeconds;
    public double postFeedRecoverySeconds = Double.NaN;
    public int recoveredDips;
    public String reason = "";

    public LauncherBurstTrial(double now, double feedSeconds) {
        this(now, feedSeconds, true);
    }

    public LauncherBurstTrial(double now, double feedSeconds, boolean burst) {
        this(now, feedSeconds, burst, TARGET_RPM);
    }

    public LauncherBurstTrial(double now, double feedSeconds, boolean burst, double targetRpm) {
        if (!Double.isFinite(targetRpm) || targetRpm < MIN_TARGET_RPM || targetRpm > MAX_TARGET_RPM) {
            throw new IllegalArgumentException("Target RPM must be 1500 to 3000");
        }
        this.targetRpm = targetRpm;
        if (!Double.isFinite(now) || !Double.isFinite(feedSeconds) || feedSeconds < 0.5 || feedSeconds > 6) {
            throw new IllegalArgumentException("Feed duration must be 0.5 to 6 seconds");
        }
        start = phaseStart = lastTime = now;
        this.feedSeconds = feedSeconds;
        this.burst = burst;
    }

    public void update(double now, double rpm) {
        if (finished()) return;
        if (!Double.isFinite(now) || now < lastTime || !Double.isFinite(rpm)) {
            abort("Invalid time or encoder reading"); return;
        }
        double dt = now - lastTime;
        lastTime = now;
        // A stalled Android loop cannot establish continuous settling or safely continue feeding.
        if (dt > 0.25) { abort("Sampling gap exceeded 250 ms"); return; }
        if (rpm < -100 || rpm > 3200) { abort("Wrong direction or RPM above 3200"); return; }
        boolean inBand = Math.abs(targetRpm - rpm) <= TOLERANCE_RPM;
        if (inBand) {
            if (Double.isNaN(bandStart)) bandStart = now;
        } else bandStart = Double.NaN;
        boolean settled = !Double.isNaN(bandStart) && now - bandStart >= SETTLE_SECONDS;

        if (phase == Phase.SPINUP || phase == Phase.BASELINE) peakSpinupRpm = Math.max(peakSpinupRpm, rpm);
        if (phase == Phase.SPINUP) {
            if (inBand && Double.isNaN(firstBandSeconds)) firstBandSeconds = now - start;
            if (settled) {
                settledSeconds = now - start;
                transition(Phase.BASELINE, now);
            } else if (now - start >= 8) abort("Spin-up did not settle within 8 seconds");
        } else if (phase == Phase.BASELINE) {
            baselineSum += rpm;
            baselineSamples++;
            baselineMin = Math.min(baselineMin, rpm);
            baselineMax = Math.max(baselineMax, rpm);
            // Require a full second continuously in band before opening the gate.
            if (!inBand) {
                phaseStart = now;
            } else if (now - phaseStart >= 1 && settled) {
                if (burst) transition(Phase.GATE_OPENING, now);
                else { reason = "Completed spin-up-only trial"; transition(Phase.DONE, now); }
            }
            if (phase == Phase.BASELINE && now - start >= 12) abort("Unstable unloaded baseline");
        } else if (phase == Phase.GATE_OPENING) {
            if (!inBand) { abort("Speed lost before feed started"); return; }
            if (now - phaseStart >= 0.25) transition(Phase.FEED, now);
        } else if (phase == Phase.FEED) {
            minFeedRpm = Double.isNaN(minFeedRpm) ? rpm : Math.min(minFeedRpm, rpm);
            double error = targetRpm - rpm;
            feedSquaredError += error * error * dt;
            feedErrorArea += Math.abs(error) * dt;
            feedObservedSeconds += dt;
            if (inBand) feedGoodSeconds += dt;
            measureDip(now, rpm, inBand);
            if (rpm < 1000) {
                if (Double.isNaN(lowStart)) lowStart = now;
                if (now - lowStart >= 0.25) { abort("RPM below 1000 for 250 ms during feed"); return; }
            } else lowStart = Double.NaN;
            if (now - phaseStart >= feedSeconds) {
                transition(Phase.RECOVERY, now);
                bandStart = Double.NaN;
            }
        } else if (phase == Phase.RECOVERY) {
            measureDip(now, rpm, inBand);
            if (settled && Double.isNaN(postFeedRecoverySeconds)) postFeedRecoverySeconds = now - phaseStart;
            // Observe for at least one second; completion also needs current stable speed.
            if (now - phaseStart >= 1 && settled) {
                reason = "Completed timed burst; verify all four POLLEN cleared";
                transition(Phase.DONE, now);
            } else if (now - phaseStart >= 4) abort("Did not recover within 4 seconds after feed");
        }
    }

    private void measureDip(double now, double rpm, boolean inBand) {
        if (rpm < targetRpm - TOLERANCE_RPM && Double.isNaN(dipStart)) dipStart = now;
        if (!Double.isNaN(dipStart)) {
            if (inBand) {
                if (Double.isNaN(dipBandStart)) dipBandStart = now;
                if (now - dipBandStart >= SETTLE_SECONDS) {
                    maxDipRecoverySeconds = Math.max(maxDipRecoverySeconds, now - dipStart);
                    recoveredDips++;
                    dipStart = dipBandStart = Double.NaN;
                }
            } else dipBandStart = Double.NaN;
        }
    }

    private void transition(Phase next, double now) { phase = next; phaseStart = now; }
    public void abort(String message) { if (!finished()) { reason = message; phase = Phase.ABORTED; } }
    public double targetRpm() { return targetRpm; }
    public Phase phase() { return phase; }
    public boolean finished() { return phase == Phase.DONE || phase == Phase.ABORTED; }
    public boolean gateOpen() { return phase == Phase.GATE_OPENING || phase == Phase.FEED; }
    public boolean feeding() { return phase == Phase.FEED; }
    public double elapsed(double now) { return now - start; }
    public boolean unrecoveredDip() { return !Double.isNaN(dipStart); }
    public double baselineMean() { return baselineSamples == 0 ? Double.NaN : baselineSum / baselineSamples; }
    public double baselineRange() { return baselineSamples == 0 ? Double.NaN : baselineMax - baselineMin; }
    public double feedRmsError() { return feedObservedSeconds == 0 ? Double.NaN : Math.sqrt(feedSquaredError / feedObservedSeconds); }
    public double feedMeanAbsoluteError() { return feedObservedSeconds == 0 ? Double.NaN : feedErrorArea / feedObservedSeconds; }
    public double feedInBandPercent() { return feedObservedSeconds == 0 ? Double.NaN : 100 * feedGoodSeconds / feedObservedSeconds; }

    /** Conservative one-variable experiment, not an identified optimal controller. */
    public String recommendation(double[] gains) {
        int index = suggestedGain(gains);
        if (index < 0) return "No automatic change: compare logs, battery, feed pressure and wheel slip.";
        String[] names = {"P", "I", "D", "F"};
        return String.format(java.util.Locale.US, "Try %s %.4f -> %.4f; repeat unchanged load/battery. Heuristic only.",
                names[index], gains[index], suggestedValue(gains));
    }

    public int suggestedGain(double[] gains) {
        if (reason.contains("direction") || reason.contains("Invalid") || reason.contains("Sampling")
                || reason.contains("Operator") || reason.contains("STOP") || reason.contains("1000")) return -1;
        if (peakSpinupRpm > targetRpm + 150 || baselineRange() > 150) return gains[1] > 0 ? 1 : 0;
        if (Double.isNaN(settledSeconds)) return -1; // No feed trial: diagnose before increasing gains.
        if (baselineMean() < targetRpm - 35) return 3;
        if (baselineMean() > targetRpm + 35) return 3;
        if (unrecoveredDip() || postFeedRecoverySeconds > 0.8 || minFeedRpm < targetRpm - 150
                || settledSeconds > 2) return 0;
        return -1;
    }

    public double suggestedValue(double[] gains) {
        int index = suggestedGain(gains);
        if (index < 0) return Double.NaN;
        boolean oscillation = peakSpinupRpm > targetRpm + 150 || baselineRange() > 150;
        if (oscillation) return gains[index] * 0.8;
        if (index == 3) return baselineMean() > targetRpm ? gains[3] * 0.95 : gains[3] * 1.05;
        return gains[0] == 0 ? 0.1 : gains[0] * 1.1;
    }
}
