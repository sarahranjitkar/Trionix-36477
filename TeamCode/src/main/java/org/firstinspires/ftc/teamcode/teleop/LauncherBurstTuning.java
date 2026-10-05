package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel;
import org.firstinspires.ftc.teamcode.mechanisms.Intake;
import org.firstinspires.ftc.teamcode.mechanisms.ServoGate;

/** Repeatable unloaded and four-POLLEN burst experiments at LauncherBurstTrial.TARGET_RPM. */
@TeleOp(name = "Launcher Burst Tuning", group = "TRIONIX")
public class LauncherBurstTuning extends LinearOpMode {
    private Flywheel launcher;
    private Intake intake;
    private ServoGate gate;
    private LauncherBurstTrial trial;
    private RunLog log;
    private double[] runGains;
    private String status = "Load four POLLEN with motors stopped. A starts an automatic burst.";

    @Override public void runOpMode() throws InterruptedException {
        try {
            launcher = new Flywheel(hardwareMap);
            intake = new Intake(hardwareMap);
            gate = new ServoGate(hardwareMap);
            // Start from the same team-approved gains used by the normal launcher.
            PIDFCoefficients initial = launcher.getPIDF();
            double[] gains = {initial.p, initial.i, initial.d, initial.f};
            double[] steps = {0.01, 0.1, 1};
            String[] names = {"P", "I", "D", "F", "Target RPM"};
            double targetRpm = LauncherBurstTrial.TARGET_RPM;
            int selected = 0, step = 1;
            double feedSeconds = 2, feedPower = 0.50;
            boolean burst = true;
            boolean suggestionApplied = false;
            File directory = new File(AppUtil.ROBOT_DATA_DIR, "launcher-tuning");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create " + directory);
            Gamepad previous = new Gamepad(), current = new Gamepad();
            telemetry.setMsTransmissionInterval(100);
            telemetry.addLine("Gate is CLOSED. Aim launcher into a clear collection area.");
            telemetry.addData("Target RPM (next run)", "%.0f", targetRpm);
            telemetry.addLine("START then A: spin up, settle, open gate, feed, recover, stop.");
            telemetry.addLine("B/BACK abort. X selects spin-up-only. Edits allowed between runs.");
            telemetry.addData("Logs", directory.getAbsolutePath());
            telemetry.update();
            waitForStart();
            previous.copy(gamepad1);
            while (opModeIsActive()) {
                current.copy(gamepad1);
                double rpm = launcher.getCurrentRpm();
                double amps = launcher.getCurrentAmps();
                double volts = voltage();
                double now = seconds();
                boolean active = trial != null && !trial.finished();
                if (current.b || current.back) {
                    if (active) trial.abort("Operator abort");
                    stopHardware();
                } else if (!active) {
                    if (current.left_bumper && !previous.left_bumper) selected = (selected + 4) % 5;
                    if (current.right_bumper && !previous.right_bumper) selected = (selected + 1) % 5;
                    if (current.right_stick_button && !previous.right_stick_button) step = (step + 1) % steps.length;
                    if (current.dpad_left && !previous.dpad_left) {
                        if (selected == 4) targetRpm = Math.max(LauncherBurstTrial.MIN_TARGET_RPM, targetRpm - 100);
                        else gains[selected] = Math.max(0, gains[selected] - steps[step]);
                    }
                    if (current.dpad_right && !previous.dpad_right) {
                        if (selected == 4) targetRpm = Math.min(LauncherBurstTrial.MAX_TARGET_RPM, targetRpm + 100);
                        else gains[selected] += steps[step];
                    }
                    if (current.dpad_up && !previous.dpad_up) feedSeconds = Math.min(6, feedSeconds + 0.25);
                    if (current.dpad_down && !previous.dpad_down) feedSeconds = Math.max(0.5, feedSeconds - 0.25);
                    if (current.left_trigger > 0.5 && previous.left_trigger <= 0.5) feedPower = Math.max(0.1, feedPower - 0.05);
                    if (current.right_trigger > 0.5 && previous.right_trigger <= 0.5) feedPower = Math.min(1, feedPower + 0.05);
                    if (current.x && !previous.x) burst = !burst;
                    if (current.y && !previous.y && trial != null && !suggestionApplied && targetRpm == trial.targetRpm()) {
                        int index = trial.suggestedGain(runGains);
                        if (index >= 0) {
                            gains[index] = trial.suggestedValue(runGains);
                            suggestionApplied = true;
                            status = "Suggested change selected for NEXT run; not applied to match code.";
                        }
                    }
                    if (current.a && !previous.a) {
                        if (!Double.isFinite(rpm) || Math.abs(rpm) > 100) {
                            status = "Wait until wheel is below 100 RPM, then press A again.";
                        } else {
                            runGains = gains.clone();
                            // Verify writable logging before commanding motor motion.
                            log = new RunLog(directory, runGains, feedSeconds, feedPower, burst);
                            gate.close();
                            intake.stop();
                            launcher.setPIDF(gains[0], gains[1], gains[2], gains[3]);
                            now = seconds();
                            trial = new LauncherBurstTrial(now, feedSeconds, burst, targetRpm);
                            suggestionApplied = false;
                            launcher.setTargetRpm(trial.targetRpm());
                            status = "Running. B/BACK abort immediately; PIDF edits are locked.";
                        }
                    }
                }
                if (trial != null && log != null) {
                    LauncherBurstTrial.Phase observed = trial.phase();
                    trial.update(now, rpm);
                    if (trial.finished()) {
                        stopHardware();
                    } else {
                        gate.setOpen(trial.gateOpen());
                        if (trial.feeding()) intake.setSpeed(feedPower); else intake.stop();
                    }
                    log.sample(trial, now, rpm, amps, volts, observed);
                    if (trial.finished()) {
                        log.finish(trial);
                        status = trial.reason + "; saved " + log.prefix;
                        log = null;
                    }
                }

                telemetry.addData("Mode", burst ? "FOUR-POLLEN TIMED BURST" : "SPIN-UP ONLY (no feed)");
                telemetry.addData("Phase", trial == null ? "READY" : trial.phase());
                telemetry.addData("Target RPM (next run)", "%.0f", targetRpm);
                telemetry.addData("RPM / A / V", "%.0f / %.2f / %.2f", rpm, amps, volts);
                telemetry.addData("Next run PIDF", "%.4f / %.4f / %.4f / %.4f", gains[0], gains[1], gains[2], gains[3]);
                telemetry.addData("Editing", "%s step %.2f", names[selected], selected == 4 ? 100 : steps[step]);
                telemetry.addData("Feed duration / power", "%.2fs / %.2f", feedSeconds, feedPower);
                if (trial != null) {
                    telemetry.addData("Results target RPM", "%.0f", trial.targetRpm());
                    telemetry.addData("First band / settled (s)", "%.3f / %.3f", trial.firstBandSeconds, trial.settledSeconds);
                    telemetry.addData("Peak spin-up / min feed RPM", "%.0f / %.0f", trial.peakSpinupRpm, trial.minFeedRpm);
                    telemetry.addData("Feed RMS error / in band", "%.1f RPM / %.1f%%", trial.feedRmsError(), trial.feedInBandPercent());
                    telemetry.addData("Post-feed stable recovery (s)", "%.3f", trial.postFeedRecoverySeconds);
                    telemetry.addData("Longest recovered dip / unresolved", "%.3fs / %s", trial.maxDipRecoverySeconds, trial.unrecoveredDip());
                    if (trial.finished()) telemetry.addData("Suggestion (Y accepts once)",
                            targetRpm == trial.targetRpm() ? trial.recommendation(runGains)
                                    : "Target changed: run a new trial before applying suggestions.");
                }
                telemetry.addLine(status);
                telemetry.addLine("A run | B/BACK abort | X burst/spin-only | Y use suggestion");
                telemetry.addLine("Bumpers: P/I/D/F/Target | Left/Right: change | RPM step: 100");
                telemetry.addLine("Right-stick: gain step | Up/Down: feed time | Triggers: feed power");
                telemetry.addLine("NaN = not measured. Four balls are NOT counted by a sensor.");
                telemetry.update();
                previous.copy(current);
                sleep(20);
            }
        } catch (IOException failure) {
            status = "Logging failed: " + failure.getMessage();
            telemetry.log().add(status);
            telemetry.update();
        } finally {
            try { stopHardware(); }
            finally {
                if (trial != null && !trial.finished()) trial.abort("Driver Station STOP or OpMode interrupted");
                if (log != null) {
                    try { log.finish(trial); }
                    catch (IOException failure) { telemetry.log().add("Log save failed: " + failure.getMessage()); }
                }
            }
        }
    }

    private void stopHardware() {
        try { if (intake != null) intake.stop(); }
        finally {
            try { if (launcher != null) launcher.stop(); }
            finally { if (gate != null) gate.close(); }
        }
    }

    private double voltage() {
        double minimum = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double value = sensor.getVoltage();
            if (value > 0 && Double.isFinite(value)) minimum = Math.min(minimum, value);
        }
        return Double.isInfinite(minimum) ? Double.NaN : minimum;
    }

    private static double seconds() { return System.nanoTime() / 1e9; }

    /** Samples buffer in RAM; disk writes happen with motors stopped. */
    private static final class RunLog {
        final String prefix;
        final File directory;
        final double[] gains;
        final double feedSeconds, power;
        final boolean burst;
        final StringBuilder samples = new StringBuilder();
        final BufferedWriter writer;
        boolean closed;

        RunLog(File directory, double[] gains, double feedSeconds, double power, boolean burst) throws IOException {
            this.directory = directory; this.gains = gains.clone();
            this.feedSeconds = feedSeconds; this.power = power; this.burst = burst;
            prefix = "trial-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            writer = new BufferedWriter(new FileWriter(new File(directory, prefix + "-samples.csv")));
            writer.write("elapsed_s,observed_phase,next_phase,target_rpm,rpm,error_rpm,launcher_amps,battery_v,gate_open_command,intake_power_command,p,i,d,f\n");
            writer.flush();
        }

        void sample(LauncherBurstTrial trial, double now, double rpm, double amps, double volts,
                    LauncherBurstTrial.Phase observed) {
            samples.append(String.format(Locale.US,
                    "%.4f,%s,%s,%.0f,%.2f,%.2f,%.3f,%.3f,%s,%.3f,%.6f,%.6f,%.6f,%.6f%n",
                    trial.elapsed(now), observed, trial.phase(), trial.targetRpm(),
                    rpm, trial.targetRpm()-rpm, amps, volts,
                    trial.gateOpen(), trial.feeding() ? power : 0, gains[0], gains[1], gains[2], gains[3]));
        }

        void finish(LauncherBurstTrial trial) throws IOException {
            if (closed) return;
            closed = true;
            try { writer.write(samples.toString()); } finally { writer.close(); }
            if (trial == null) return;
            try (BufferedWriter summary = new BufferedWriter(new FileWriter(new File(directory, prefix + "-summary.csv")))) {
                summary.write("target_rpm,mode,result,reason,p,i,d,f,feed_seconds,intake_power,first_band_s,settled_s,peak_spinup_rpm,baseline_mean_rpm,baseline_range_rpm,min_feed_rpm,feed_rms_error_rpm,feed_mean_abs_error_rpm,feed_in_band_pct,post_feed_recovery_s,max_recovered_dip_s,recovered_dips,unresolved_dip,suggestion\n");
                summary.write(String.format(Locale.US,
                        "%.0f,%s,%s,%s,%.6f,%.6f,%.6f,%.6f,%.2f,%.3f,%.4f,%.4f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.4f,%.4f,%d,%s,%s%n",
                        trial.targetRpm(), burst ? "burst" : "spinup", trial.phase(), csv(trial.reason), gains[0], gains[1], gains[2], gains[3],
                        feedSeconds, power, trial.firstBandSeconds, trial.settledSeconds, trial.peakSpinupRpm,
                        trial.baselineMean(), trial.baselineRange(), trial.minFeedRpm, trial.feedRmsError(),
                        trial.feedMeanAbsoluteError(), trial.feedInBandPercent(), trial.postFeedRecoverySeconds,
                        trial.maxDipRecoverySeconds, trial.recoveredDips, trial.unrecoveredDip(), csv(trial.recommendation(gains))));
            }
        }

        private static String csv(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }
    }
}
