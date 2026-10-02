package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.mechanisms.Flywheel;
import org.firstinspires.ftc.teamcode.mechanisms.Intake;
import org.firstinspires.ftc.teamcode.mechanisms.ServoGate;
import org.firstinspires.ftc.teamcode.pedro.Constants;

@TeleOp(name = "Driver Controlled", group = "TRIONIX")
public class DriverControlled extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Mecanum drive = Constants.createDrivetrain(hardwareMap);
        Intake intake = null;
        Flywheel launcher = null;
        ServoGate gate = null;
        try {
            gate = new ServoGate(hardwareMap); // Requires calibrated endpoints; starts CLOSED.
            intake = new Intake(hardwareMap);
            launcher = new Flywheel(hardwareMap);
            displayControls();
            gate.displayTelemetry(telemetry);
            intake.displayTelemetry(telemetry);
            launcher.displayTelemetry(telemetry);
            telemetry.update();
            waitForStart();
            Gamepad current = new Gamepad();
            current.copy(gamepad1);
            DriverInputs inputs = new DriverInputs(current);
            // A held through START must be released and pressed again to open.
            boolean previousGateOpen = current.a;
            while (opModeIsActive()) {
                current.copy(gamepad1);
                inputs.update(current);
                boolean gateOpenPressed = current.a;
                updateGateControls(gate, gateOpenPressed, current.b, previousGateOpen);
                previousGateOpen = gateOpenPressed;
                // Pedro uses positive left strafe and counterclockwise rotation.
                drive.drive(new DrivePowers(-current.left_stick_y,
                        -current.left_stick_x, -current.right_stick_x), true);
                intake.setSpeed(inputs.intakeDirection * Constants.INTAKE_POWER);
                if (inputs.launcherRunning) launcher.setTargetRpm(inputs.selectedRpm);
                else launcher.stop();
                idle();
                displayControls();
                telemetry.addData("Intake", inputs.intakeDirection == 0 ? "OFF"
                        : inputs.intakeDirection > 0 ? "FORWARD" : "REVERSE");
                telemetry.addData("Launcher selected RPM", "%.0f", inputs.selectedRpm);
                gate.displayTelemetry(telemetry);
                intake.displayTelemetry(telemetry);
                launcher.displayTelemetry(telemetry);
                telemetry.update();
            }
        } finally {
            try {
                // Request closure, but STOP/power loss cannot guarantee physical travel.
                if (gate != null) gate.close();
            } finally {
                drive.stop();
                if (intake != null) intake.stop();
                if (launcher != null) launcher.stop();
            }
        }
    }

    private void displayControls() {
        telemetry.addLine("Left stick: drive. Right stick: turn.");
        telemetry.addLine("Left bumper: intake forward/off. Right bumper: reverse/off.");
        telemetry.addLine("Both bumpers: intake OFF.");
        telemetry.addLine("Dpad Up/Down: +/-100 RPM per press (starts launcher).");
        telemetry.addLine("Dpad Left: resume selected RPM. Dpad Right: STOP (priority).");
        telemetry.addLine("A: gate OPEN. B: gate CLOSED (priority).");
    }

    /** Button edges are separate from actuator writes, so loop speed cannot change a step. */
    static final class DriverInputs {
        private final Gamepad previous = new Gamepad();
        int intakeDirection;
        double selectedRpm;
        boolean launcherRunning;

        DriverInputs(Gamepad initial) { previous.copy(initial); }

        void update(Gamepad current) {
            if (current.left_bumper && current.right_bumper) {
                intakeDirection = 0;
            } else if (current.left_bumper && !previous.left_bumper) {
                intakeDirection = intakeDirection == 1 ? 0 : 1;
            } else if (current.right_bumper && !previous.right_bumper) {
                intakeDirection = intakeDirection == -1 ? 0 : -1;
            }

            if (current.dpad_right) {
                launcherRunning = false; // Stop wins over run and speed changes.
            } else {
                // Opposing speed buttons cancel each other while both are held.
                if (current.dpad_up && !current.dpad_down && !previous.dpad_up) {
                    selectedRpm = Math.min(Flywheel.MAX_RPM, selectedRpm + Flywheel.RPM_STEP);
                    launcherRunning = selectedRpm > 0;
                } else if (current.dpad_down && !current.dpad_up && !previous.dpad_down) {
                    selectedRpm = Math.max(0, selectedRpm - Flywheel.RPM_STEP);
                    launcherRunning = selectedRpm > 0;
                }
                if (current.dpad_left && !previous.dpad_left) launcherRunning = selectedRpm > 0;
            }
            // Always consume edges, including buttons held during Stop.
            previous.copy(current);
        }
    }

    static void updateGateControls(ServoGate gate, boolean openPressed,
                                   boolean closePressed, boolean previousOpenPressed) {
        if (closePressed) {
            gate.close();
        } else if (openPressed && !previousOpenPressed) {
            gate.open();
        }
    }
}
