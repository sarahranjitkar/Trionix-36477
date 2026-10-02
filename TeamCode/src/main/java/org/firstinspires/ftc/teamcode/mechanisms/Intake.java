package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.pedro.Constants;

public final class Intake {
    private final DcMotorEx motor;

    public Intake(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, Constants.INTAKE_NAME);
        motor.setPower(0);
        motor.setDirection(DcMotorSimple.Direction.FORWARD);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public void start() { motor.setPower(Constants.INTAKE_POWER); }
    public void setSpeed(double power) {motor.setPower(power);}
    public void stop() { motor.setPower(0); }

    public double getCurrentAmps() { return motor.getCurrent(CurrentUnit.AMPS); }

    /** Adds measured motor current; the calling OpMode owns telemetry.update(). */
    public void displayTelemetry(Telemetry telemetry) {
        telemetry.addData("Intake current (A)", "%.2f", getCurrentAmps());
    }
}
