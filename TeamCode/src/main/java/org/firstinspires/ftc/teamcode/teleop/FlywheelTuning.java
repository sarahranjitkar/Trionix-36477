package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel;

/** Driver Station PIDF tuner. Copy the displayed gains to Flywheel.TUNED_PIDF. */
@TeleOp(name = "Flywheel PIDF Tuning", group = "TRIONIX")
public class FlywheelTuning extends LinearOpMode {
    private static final String[] GAIN_NAMES = {"P", "I", "D", "F"};
    private static final double[] GAIN_STEPS = {0.01, 0.1, 1.0};

    @Override
    public void runOpMode() throws InterruptedException {
        Flywheel flywheel = new Flywheel(hardwareMap);
        try {
            PIDFCoefficients initial = flywheel.getPIDF();
            double[] gains = {initial.p, initial.i, initial.d, initial.f};
            int selectedGain = 0;
            int stepIndex = 1;
            double selectedRpm = 1000;
            boolean running = false;
            Gamepad previous = new Gamepad();
            Gamepad current = new Gamepad();
            telemetry.setMsTransmissionInterval(100);
            telemetry.addLine("Motor stays OFF until START, then A. B stops.");
            telemetry.addData("Motor", "%s: 5203 1:1, 28 counts/rev", Flywheel.MOTOR_NAME);
            telemetry.addLine("Connect encoder; select goBILDA 6000 RPM motor in configuration.");
            telemetry.addLine("Keep launcher clear for the initial unloaded test.");
            telemetry.update();
            waitForStart();
            // A held before START must be released and pressed again to run.
            previous.copy(gamepad1);
            while (opModeIsActive()) {
                current.copy(gamepad1);
                if (current.dpad_up && !previous.dpad_up) {
                    selectedRpm = Math.min(Flywheel.MAX_RPM, selectedRpm + Flywheel.RPM_STEP);
                }
                if (current.dpad_down && !previous.dpad_down) {
                    selectedRpm = Math.max(0, selectedRpm - Flywheel.RPM_STEP);
                }
                if (current.right_bumper && !previous.right_bumper) selectedGain = (selectedGain + 1) % 4;
                if (current.left_bumper && !previous.left_bumper) selectedGain = (selectedGain + 3) % 4;
                if (current.y && !previous.y) stepIndex = (stepIndex + 1) % GAIN_STEPS.length;
                boolean gainsChanged = false;
                if (current.dpad_right && !previous.dpad_right) {
                    gains[selectedGain] += GAIN_STEPS[stepIndex];
                    gainsChanged = true;
                }
                if (current.dpad_left && !previous.dpad_left) {
                    gains[selectedGain] = Math.max(0, gains[selectedGain] - GAIN_STEPS[stepIndex]);
                    gainsChanged = true;
                }
                // Stop wins over simultaneous A and B, including while editing gains.
                if (current.b) {
                    running = false;
                    flywheel.stop();
                } else if (current.a && !previous.a) {
                    running = true;
                }
                if (gainsChanged) flywheel.setPIDF(gains[0], gains[1], gains[2], gains[3]);
                if (running) {
                    flywheel.setTargetRpm(selectedRpm);
                    if (selectedRpm == 0) running = false;
                }

                telemetry.addData("State", running ? "RUNNING" : "OFF (coasting if still spinning)");
                telemetry.addData("Selected RPM", "%.0f", selectedRpm);
                flywheel.displayTelemetry(telemetry);
                telemetry.addData("Within 100 RPM (instantaneous)", flywheel.isAtTargetRpm(100));
                telemetry.addData("Editing", "%s, step %.2f", GAIN_NAMES[selectedGain], GAIN_STEPS[stepIndex]);
                telemetry.addData("P / I / D / F", "%.4f / %.4f / %.4f / %.4f",
                        gains[0], gains[1], gains[2], gains[3]);
                telemetry.addData("Copy to TUNED_PIDF", "new PIDFCoefficients(%.4f, %.4f, %.4f, %.4f);",
                        gains[0], gains[1], gains[2], gains[3]);
                telemetry.addLine("A: run | B: stop | Up/Down: RPM +/-100");
                telemetry.addLine("Bumpers: select P/I/D/F | Left/Right: gain -/+ | Y: gain step");
                telemetry.addLine("Gains are temporary: copy into Flywheel.java and redeploy.");
                telemetry.update();
                previous.copy(current);
                sleep(20);
            }
        } finally {
            flywheel.stop();
        }
    }
}
