package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import java.util.Locale;
import org.firstinspires.ftc.teamcode.mechanisms.ServoGate;

/** Manually find two endpoints, then copy them into ServoGate.java. */
@TeleOp(name = "Servo Gate Calibration", group = "TRIONIX")
public class ServoGateCalibration extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        ServoImplEx servo = hardwareMap.get(ServoImplEx.class, ServoGate.SERVO_NAME);
        try {
            ServoGate.configureServo(servo);
            double position = 0.5;
            double closed = ServoGate.CLOSED_POSITION;
            double open = ServoGate.OPEN_POSITION;
            boolean enabled = false;
            boolean fine = false;
            Gamepad previous = new Gamepad();
            Gamepad current = new Gamepad();
            telemetry.setMsTransmissionInterval(100);
            telemetry.addLine("PWM disabled. No launcher or intake is driven by this tuner.");
            telemetry.addLine("First setup: remove horn/linkage before centering.");
            telemetry.addLine("START, then A commands 0.500. BACK releases servo holding torque.");
            telemetry.update();
            waitForStart();
            previous.copy(gamepad1);
            while (opModeIsActive()) {
                current.copy(gamepad1);
                // Release wins over every movement/record command.
                if (current.back) {
                    servo.setPwmDisable();
                    enabled = false;
                } else {
                    boolean move = false;
                    if (current.a && !previous.a && !enabled) {
                        enabled = true;
                        move = true;
                    }
                    if (current.left_bumper && !previous.left_bumper) fine = !fine;
                    if (enabled) {
                        double step = fine ? 0.002 : 0.01;
                        // Closed recall wins over open recall and nudges.
                        if (current.b) {
                            if (!Double.isNaN(closed)) { position = closed; move = true; }
                        } else if (current.right_bumper) {
                            if (!Double.isNaN(open)) { position = open; move = true; }
                        } else if (current.dpad_left && !previous.dpad_left) {
                            position = Math.max(0, position - step);
                            move = true;
                        } else if (current.dpad_right && !previous.dpad_right) {
                            position = Math.min(1, position + step);
                            move = true;
                        }
                        if (move) {
                            servo.setPosition(position);
                            servo.setPwmEnable();
                        }
                        if (current.x && !previous.x) closed = position;
                        if (current.y && !previous.y) open = position;
                    }
                }
                telemetry.addData("Servo output", enabled ? "ENABLED" : "RELEASED - A to enable");
                telemetry.addData("Command position (not measured)", "%.3f", position);
                telemetry.addData("Nudge step", fine ? "0.002" : "0.010");
                telemetry.addData("CLOSED_POSITION", formatPosition(closed));
                telemetry.addData("OPEN_POSITION", formatPosition(open));
                if (!Double.isNaN(closed) && !Double.isNaN(open)) {
                    telemetry.addLine(closed == open ? "Endpoints must differ; record again."
                            : "Record both values, paste into ServoGate.java, then redeploy.");
                }
                telemetry.addLine("A: enable at displayed value | BACK: release PWM");
                telemetry.addLine("D-pad Left/Right: nudge | Left bumper: fine/coarse");
                telemetry.addLine("X: record CLOSED | Y: record OPEN (session only)");
                telemetry.addLine("B: recall CLOSED | Right bumper: recall OPEN (if recorded)");
                telemetry.addLine("STOP releases PWM; it does not close or lock the gate.");
                telemetry.update();
                previous.copy(current);
                sleep(20);
            }
        } finally {
            // Never jump to an unverified endpoint when calibration ends.
            servo.setPwmDisable();
        }
    }

    private static String formatPosition(double position) {
        return Double.isNaN(position) ? "NOT RECORDED" : String.format(Locale.US, "%.3f", position);
    }
}
