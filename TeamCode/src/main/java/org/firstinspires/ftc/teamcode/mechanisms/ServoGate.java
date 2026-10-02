package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Two-position POLLEN gate for a goBILDA 2000 series servo in positional mode. */
public final class ServoGate {
    public static final String SERVO_NAME = "Servo Gate";
    public static final Servo.Direction DIRECTION = Servo.Direction.FORWARD;

    // Run Servo Gate Calibration, then replace BOTH values with measured positions.
    // NaN deliberately prevents a match OpMode from using uncalibrated endpoints.
    public static final double CLOSED_POSITION = Double.NaN;
    public static final double OPEN_POSITION = Double.NaN;

    private final Servo servo;
    private final double closedPosition;
    private final double openPosition;
    private boolean openCommanded;

    /** Commands CLOSED immediately on initialization, after validating calibration. */
    public ServoGate(HardwareMap hardwareMap) {
        this(hardwareMap.get(Servo.class, SERVO_NAME), CLOSED_POSITION, OPEN_POSITION);
    }

    public ServoGate(Servo servo, double closedPosition, double openPosition) {
        requirePosition(closedPosition);
        requirePosition(openPosition);
        if (closedPosition == openPosition) {
            throw new IllegalArgumentException("Gate OPEN and CLOSED positions must differ");
        }
        this.servo = servo;
        this.closedPosition = closedPosition;
        this.openPosition = openPosition;
        configureServo(servo);
        close();
    }

    /** Shared pulse mapping for the gate and its tuner. Leaves this servo's PWM disabled.
     * Both inventory models (2000-0025-0002 and -0003) accept 500-2500 us in Servo Mode.
     */
    public static void configureServo(Servo servo) {
        if (!(servo instanceof PwmControl)) {
            throw new IllegalArgumentException("Servo Gate requires a REV Hub positional servo port");
        }
        PwmControl pwm = (PwmControl) servo;
        pwm.setPwmDisable();
        servo.setDirection(DIRECTION);
        servo.scaleRange(0, 1);
        pwm.setPwmRange(new PwmControl.PwmRange(500, 2500));
    }

    /** Commands the passage open; returns before the servo finishes moving. */
    public void open() { command(openPosition, true); }

    /** Commands the passage closed; returns before the servo finishes moving. */
    public void close() { command(closedPosition, false); }

    public void setOpen(boolean open) {
        if (open) open(); else close();
    }

    /** Commanded state only. Standard three-wire servos provide no position feedback. */
    public boolean isOpenCommanded() { return openCommanded; }

    public double getCommandedPosition() {
        return openCommanded ? openPosition : closedPosition;
    }

    /** Caller owns telemetry.update(). These readings are commands, not sensor data. */
    public void displayTelemetry(Telemetry telemetry) {
        telemetry.addData("Gate command", openCommanded ? "OPEN" : "CLOSED");
        telemetry.addData("Gate commanded position", "%.3f", getCommandedPosition());
    }

    private void command(double position, boolean open) {
        servo.setPosition(position);
        ((PwmControl) servo).setPwmEnable();
        openCommanded = open;
    }

    private static void requirePosition(double position) {
        if (Double.isNaN(position) || Double.isInfinite(position) || position < 0 || position > 1) {
            throw new IllegalArgumentException(
                    "Gate positions must be calibrated in [0, 1]. Run Servo Gate Calibration first.");
        }
    }
}
