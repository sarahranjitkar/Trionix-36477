package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/** Encoder velocity control for the goBILDA 5203 1:1 launcher motor.
 * RPM refers to the motor output shaft, before any external belt/gear ratio.
 * The Hub runs PIDF continuously; no periodic update method is required.
 */
public final class Flywheel {
    // Match the name in the Driver Station robot configuration.
    public static final String MOTOR_NAME = "Launcher";
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.FORWARD;
    public static final double TICKS_PER_REV = 28.0;
    public static final double MAX_RPM = 6000.0;
    public static final double RPM_STEP = 100.0;

    // Keep Hub defaults until tuned. Replace null with the tuner's displayed
    // new PIDFCoefficients(p, i, d, f) to reapply the result on every initialization.
    public static final PIDFCoefficients TUNED_PIDF = null;

    private final DcMotorEx motor;
    private final double ticksPerRev;
    private final double maxRpm;
    private double targetRpm;

    public Flywheel(HardwareMap hardwareMap) {
        this(hardwareMap.get(DcMotorEx.class, MOTOR_NAME), DIRECTION,
                TICKS_PER_REV, MAX_RPM);
        if (TUNED_PIDF != null) {
            setPIDF(TUNED_PIDF.p, TUNED_PIDF.i, TUNED_PIDF.d, TUNED_PIDF.f);
        }
    }

    /** Explicit motor/calibration constructor for alternate configurations. */
    public Flywheel(DcMotorEx motor, DcMotorSimple.Direction direction,
                    double ticksPerRev, double maxRpm) {
        requirePositive(ticksPerRev, "ticksPerRev");
        requirePositive(maxRpm, "maxRpm");
        if (motor == null || direction == null) {
            throw new IllegalArgumentException("Motor and direction are required");
        }
        this.motor = motor;
        this.ticksPerRev = ticksPerRev;
        this.maxRpm = maxRpm;
        motor.setPower(0);
        motor.setDirection(direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        stop();
    }

    /** Immediately commands RPM, clamped to [0, maxRpm]. Zero coasts to a stop. */
    public void setTargetRpm(double rpm) {
        requireFinite(rpm, "rpm");
        double boundedRpm = Math.max(0, Math.min(maxRpm, rpm));
        if (boundedRpm == 0) {
            stop();
        } else {
            motor.setVelocity(boundedRpm * ticksPerRev / 60.0);
            targetRpm = boundedRpm;
        }
    }

    /** Immediately changes the commanded speed; increasing from zero starts the motor. */
    public void increaseRpm(double amount) {
        requireNonnegative(amount, "amount");
        setTargetRpm(targetRpm + Math.min(amount, maxRpm));
    }

    public void decreaseRpm(double amount) {
        requireNonnegative(amount, "amount");
        setTargetRpm(targetRpm - Math.min(amount, maxRpm));
    }

    public void increaseRpm() { increaseRpm(RPM_STEP); }
    public void decreaseRpm() { decreaseRpm(RPM_STEP); }
    public double getTargetRpm() { return targetRpm; }
    public double getCurrentRpm() { return motor.getVelocity() * 60.0 / ticksPerRev; }
    public double getCurrentAmps() { return motor.getCurrent(CurrentUnit.AMPS); }

    /** Instantaneous speed check, not a guarantee that speed has settled. */
    public boolean isAtTargetRpm(double toleranceRpm) {
        requireNonnegative(toleranceRpm, "toleranceRpm");
        return targetRpm > 0 && Math.abs(targetRpm - getCurrentRpm()) <= toleranceRpm;
    }

    public void setPIDF(double p, double i, double d, double f) {
        requireNonnegative(p, "P");
        requireNonnegative(i, "I");
        requireNonnegative(d, "D");
        requireNonnegative(f, "F");
        motor.setVelocityPIDFCoefficients(p, i, d, f);
    }

    public PIDFCoefficients getPIDF() {
        return motor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    /** Adds readings to telemetry. The calling OpMode calls telemetry.update(). */
    public void displayTelemetry(Telemetry telemetry) {
        double rpm = getCurrentRpm();
        telemetry.addData("Flywheel target RPM", "%.0f", targetRpm);
        telemetry.addData("Flywheel current RPM", "%.0f", rpm);
        telemetry.addData("Flywheel error RPM", "%.0f", targetRpm - rpm);
        telemetry.addData("Launcher current (A)", "%.2f", getCurrentAmps());
    }

    /** Removes motor power. FLOAT lets the launcher coast; it does not stop instantly. */
    public void stop() {
        motor.setPower(0);
        targetRpm = 0;
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireNonnegative(double value, String name) {
        requireFinite(value, name);
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
    }

    private static void requirePositive(double value, String name) {
        requireFinite(value, name);
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
}
