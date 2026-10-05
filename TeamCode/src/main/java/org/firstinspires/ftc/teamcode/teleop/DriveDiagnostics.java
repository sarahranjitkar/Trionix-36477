package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Isolates joystick/drivetrain performance without initializing mechanisms or gate. */
@TeleOp(name = "Drive Diagnostics", group = "TRIONIX")
public class DriveDiagnostics extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        List<DcMotor> allMotors = hardwareMap.getAll(DcMotor.class);
        Mecanum drive = null;
        try {
            // Explicitly stop intake/launcher too; this OpMode only drives the four wheels.
            for (DcMotor motor : allMotors) motor.setPower(0);
            drive = Constants.createDrivetrain(hardwareMap);
            String[] names = {Constants.drivetrainConfig.frontLeftName.get(),
                    Constants.drivetrainConfig.frontRightName.get(),
                    Constants.drivetrainConfig.backLeftName.get(),
                    Constants.drivetrainConfig.backRightName.get()};
            String[] labels = {"Front left", "Front right", "Back left", "Back right"};
            DcMotorEx[] motors = new DcMotorEx[4];
            for (int i = 0; i < motors.length; i++) {
                motors[i] = hardwareMap.get(DcMotorEx.class, names[i]);
            }
            Gamepad pad = new Gamepad();
            telemetry.setMsTransmissionInterval(250);
            telemetry.addLine("Drive only: launcher/intake OFF. Gate not initialized.");
            telemetry.addLine("Left stick: drive; right stick: turn. Hold B to stop wheels.");
            telemetry.addLine("Use clear space. Pure full forward should command +1.00 on all wheels.");
            telemetry.addData("Hub supply (V)", "%.2f", batteryVoltage());
            telemetry.update();
            waitForStart();
            long windowStart = System.nanoTime();
            long previousLoop = windowStart;
            double maxLoopMs = 0;
            int loops = 0;
            while (opModeIsActive()) {
                long now = System.nanoTime();
                maxLoopMs = Math.max(maxLoopMs, (now - previousLoop) / 1_000_000.0);
                previousLoop = now;
                loops++;
                pad.copy(gamepad1);
                // Identical signs and mixing to Driver Controlled; B commands zero power.
                DrivePowers powers = pad.b ? DrivePowers.zero()
                        : new DrivePowers(-pad.left_stick_y, -pad.left_stick_x, -pad.right_stick_x);
                drive.drive(powers, true);
                if (now - windowStart >= 250_000_000L) {
                    telemetry.addData("Stick forward / strafe / turn", "%.2f / %.2f / %.2f",
                            -pad.left_stick_y, -pad.left_stick_x, -pad.right_stick_x);
                    telemetry.addData("Hub supply (V)", "%.2f", batteryVoltage());
                    telemetry.addData("Loop avg / max (ms)", "%.1f / %.1f",
                            (now - windowStart) / 1_000_000.0 / loops, maxLoopMs);
                    for (int i = 0; i < motors.length; i++) {
                        telemetry.addData(labels[i], "cmd %.2f | speed %.0f ticks/s | %.2f A",
                                drive.wheelPowers[i], motors[i].getVelocity(),
                                motors[i].getCurrent(CurrentUnit.AMPS));
                    }
                    telemetry.addLine("Wheel power is a command, not measured output voltage.");
                    telemetry.addLine("Launcher/intake OFF. Hold B: wheels stop. DS STOP: exit.");
                    telemetry.addLine("Readings sampled at 4 Hz; short spikes can be missed.");
                    telemetry.update();
                    windowStart = now;
                    loops = 0;
                    maxLoopMs = 0;
                }
                idle();
            }
        } finally {
            try {
                if (drive != null) drive.stop();
            } finally {
                for (DcMotor motor : allMotors) motor.setPower(0);
            }
        }
    }

    private double batteryVoltage() {
        double voltage = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double measured = sensor.getVoltage();
            if (measured > 0 && !Double.isInfinite(measured)) voltage = Math.min(voltage, measured);
        }
        return Double.isInfinite(voltage) ? Double.NaN : voltage;
    }
}
