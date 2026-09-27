package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.mechanisms.Flywheel;
import org.firstinspires.ftc.teamcode.mechanisms.Intake;
import org.firstinspires.ftc.teamcode.pedro.Constants;

@TeleOp(name = "Driver Controlled", group = "TRIONIX")
public class DriverControlled extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Mecanum drive = Constants.createDrivetrain(hardwareMap);
        Intake intake = null;
        Flywheel launcher = null;
        try {
            intake = new Intake(hardwareMap);
            launcher = new Flywheel(hardwareMap);
            telemetry.addLine("Left stick: drive. Right stick: turn.");
            telemetry.addLine("Left bumper: intake on. Right bumper: intake off.");
            telemetry.addLine("Left Trigger: flywheel. Dpad Up/Down: Increase/Decrease Launcher Power");
            telemetry.update();
            waitForStart();
            boolean previousOnIn = false;
            boolean previousOnOut = false;
            while (opModeIsActive()) {
                double rpm = launcher.getTargetRpm();
                // Pedro uses positive left strafe and counterclockwise rotation.
                drive.drive(new DrivePowers(-gamepad1.left_stick_y,
                        -gamepad1.left_stick_x, -gamepad1.right_stick_x), true);
                if (gamepad1.left_bumper)
                    if (!previousOnIn) {
                        intake.start();
                        launcher.setTargetRpm(rpm);
                    } else if (previousOnIn){
                        intake.stop();
                        launcher.stop();
                    }
                    previousOnIn = !previousOnIn;
                if (gamepad1.right_bumper)
                    if (!previousOnOut) {
                        intake.setSpeed((-1 * Constants.INTAKE_POWER));
                        launcher.setTargetRpm(rpm);
                    } else if (previousOnOut){
                        intake.stop();
                        launcher.stop();
                    }
                    previousOnOut = !previousOnOut;
                /*if (gamepad1.left_trigger > 0.2) launcher.start(LAUNCHER_POWER);
                if (gamepad1.left_trigger <= 0.2) launcher.stop();*/
                if (gamepad1.dpad_up) launcher.increaseRpm();
                if (gamepad1.dpad_down) launcher.decreaseRpm();
                idle();
                telemetry.addLine("Left stick: drive. Right stick: turn.");
                telemetry.addLine("Left bumper: intake on. Right bumper: intake off.");
                telemetry.addLine("Left Trigger: flywheel. Dpad Up/Down: Increase/Decrease Launcher Power");
                launcher.displayTelemetry(telemetry);
                telemetry.update();
            }
        } finally {
            drive.stop();
            if (intake != null) intake.stop();
            if (launcher != null) launcher.stop();
        }
    }
}
