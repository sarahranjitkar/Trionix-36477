package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Intake;

import dev.nextftc.core.commands.Command;
import dev.nextftc.core.components.BindingsComponent;
import dev.nextftc.core.components.SubsystemComponent;
import dev.nextftc.ftc.Gamepads;
import dev.nextftc.ftc.NextFTCOpMode;
import dev.nextftc.ftc.components.BulkReadComponent;
import dev.nextftc.hardware.driving.MecanumDriverControlled;
import dev.nextftc.hardware.impl.MotorEx;

@TeleOp (name = "Driver Controlled")
public class DriverControlled extends NextFTCOpMode {
    public DriverControlled() {
        addComponents(
                new SubsystemComponent(Intake.INSTANCE),
                BulkReadComponent.INSTANCE,
                BindingsComponent.INSTANCE
        );
    }
    private final MotorEx frontLeftMotor = new MotorEx("Front Left Motor").reversed();
    private final MotorEx frontRightMotor = new MotorEx("Front Right Motor");
    private final MotorEx backLeftMotor = new MotorEx("Back Left Motor").reversed();
    private final MotorEx backRightMotor = new MotorEx("Back Right Motor");

    @Override
    public void onStartButtonPressed() {
        //Drivetrain
        Command driverControlled = new MecanumDriverControlled(
                frontLeftMotor,
                frontRightMotor,
                backLeftMotor,
                backRightMotor,
                () -> -1 * (double) gamepad1.left_stick_y, // Java lambda reading standard FTC gamepad values
                () -> (double) gamepad1.left_stick_x,
                () -> (double) gamepad1.right_stick_x
        );
        driverControlled.schedule();

        //Intake start and stop
        Gamepads.gamepad1().leftBumper()
                .whenBecomesTrue(Intake.INSTANCE.start);

        Gamepads.gamepad1().rightBumper()
                .whenBecomesTrue(Intake.INSTANCE.stop);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }
}
