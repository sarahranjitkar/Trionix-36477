package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.mechanisms.Intake;
import org.firstinspires.ftc.teamcode.pedro.Constants;

@TeleOp(name = "Driver Controlled", group = "TRIONIX")
public class DriverControlled extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Mecanum drive = Constants.createDrivetrain(hardwareMap);
        Intake intake = null;
        try {
            intake = new Intake(hardwareMap);
            telemetry.addLine("Left stick: drive. Right stick: turn.");
            telemetry.addLine("Left bumper: intake on. Right bumper: intake off.");
            telemetry.update();
            waitForStart();
            boolean previousOn = false;
            while (opModeIsActive()) {
                // Pedro uses positive left strafe and counterclockwise rotation.
                drive.drive(new DrivePowers(-gamepad1.left_stick_y,
                        -gamepad1.left_stick_x, -gamepad1.right_stick_x), true);
                if (gamepad1.left_bumper && !previousOn) intake.start();
                if (gamepad1.right_bumper) intake.stop();
                previousOn = gamepad1.left_bumper;
                idle();
            }
        } finally {
            drive.stop();
            if (intake != null) intake.stop();
        }
    }
}
