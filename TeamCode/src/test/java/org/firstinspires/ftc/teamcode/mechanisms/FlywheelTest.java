package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlywheelTest {
    private final FakeMotor hardware = new FakeMotor();
    private Flywheel flywheel;

    @Before public void setUp() {
        flywheel = new Flywheel(hardware.motor, DcMotorSimple.Direction.REVERSE, 28, 6000);
    }

    @Test public void initializesStoppedWithEncoderFeedbackAndCoasting() {
        assertEquals(0, hardware.power, 0);
        assertEquals(0, flywheel.getTargetRpm(), 0);
        assertEquals(DcMotor.RunMode.RUN_USING_ENCODER, hardware.mode);
        assertEquals(DcMotor.ZeroPowerBehavior.FLOAT, hardware.zeroPower);
        assertEquals(DcMotorSimple.Direction.REVERSE, hardware.direction);
        assertEquals(0, hardware.velocityCommands);
    }

    @Test public void convertsRpmToEncoderVelocityAndBack() {
        flywheel.setTargetRpm(3000);
        assertEquals(1400, hardware.commandedVelocity, 1e-9);
        hardware.measuredVelocity = 1386;
        assertEquals(2970, flywheel.getCurrentRpm(), 1e-9);
        assertTrue(flywheel.isAtTargetRpm(50));
        assertFalse(flywheel.isAtTargetRpm(20));
        hardware.measuredVelocity = -1400;
        assertEquals(-3000, flywheel.getCurrentRpm(), 1e-9);
        assertFalse(flywheel.isAtTargetRpm(50));
    }

    @Test public void clampsSpeedAndAdjustsInRpm() {
        flywheel.setTargetRpm(5900);
        flywheel.increaseRpm(500);
        assertEquals(6000, flywheel.getTargetRpm(), 0);
        assertEquals(2800, hardware.commandedVelocity, 1e-9);
        flywheel.decreaseRpm();
        assertEquals(5900, flywheel.getTargetRpm(), 0);
        flywheel.decreaseRpm(10000);
        assertEquals(0, flywheel.getTargetRpm(), 0);
        assertEquals(0, hardware.power, 0);
        flywheel.increaseRpm();
        assertEquals(100, flywheel.getTargetRpm(), 0);
        flywheel.setTargetRpm(-100);
        assertEquals(0, flywheel.getTargetRpm(), 0);
    }

    @Test public void stopRemovesPowerEvenWhileEncoderStillReportsMotion() {
        flywheel.setTargetRpm(3000);
        hardware.measuredVelocity = 1400;
        flywheel.stop();
        assertEquals(0, hardware.power, 0);
        assertEquals(0, flywheel.getTargetRpm(), 0);
        assertEquals(3000, flywheel.getCurrentRpm(), 1e-9);
        assertFalse(flywheel.isAtTargetRpm(6000));
    }

    @Test public void appliesAllFourVelocityGains() {
        flywheel.setPIDF(10, 0.1, 0.2, 12);
        PIDFCoefficients pidf = flywheel.getPIDF();
        assertEquals(10, pidf.p, 0);
        assertEquals(0.1, pidf.i, 0);
        assertEquals(0.2, pidf.d, 0);
        assertEquals(12, pidf.f, 0);
        assertEquals(DcMotor.RunMode.RUN_USING_ENCODER, hardware.pidfMode);
    }

    @Test public void telemetryReportsMeasuredSpeedAndAmpsWithoutFlushingOtherTelemetry() {
        Map<String, Object[]> readings = new HashMap<>();
        Telemetry telemetry = (Telemetry) Proxy.newProxyInstance(Telemetry.class.getClassLoader(),
                new Class<?>[]{Telemetry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addData")) {
                        readings.put((String) args[0], (Object[]) args[2]);
                        return null;
                    }
                    throw new AssertionError("Unexpected telemetry call: " + method.getName());
                });
        flywheel.setTargetRpm(3000);
        hardware.measuredVelocity = 1400;
        hardware.current = 1.75;
        flywheel.displayTelemetry(telemetry);
        assertEquals(3000, (double) readings.get("Flywheel current RPM")[0], 0);
        assertEquals(1.75, (double) readings.get("Flywheel current (A)")[0], 0);
        assertEquals(0, (double) readings.get("Flywheel error RPM")[0], 0);
        assertEquals(CurrentUnit.AMPS, hardware.currentUnit);
    }

    @Test public void rejectsInvalidCommandsWithoutSendingThemToMotor() {
        flywheel.setTargetRpm(1000);
        int sent = hardware.velocityCommands;
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> flywheel.setTargetRpm(invalid));
            assertThrows(IllegalArgumentException.class, () -> flywheel.increaseRpm(invalid));
            assertThrows(IllegalArgumentException.class, () -> flywheel.setPIDF(1, 0, 0, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> flywheel.increaseRpm(-1));
        assertThrows(IllegalArgumentException.class, () -> flywheel.decreaseRpm(-1));
        assertThrows(IllegalArgumentException.class, () -> flywheel.setPIDF(-1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> flywheel.isAtTargetRpm(-1));
        assertEquals(sent, hardware.velocityCommands);
        assertEquals(1000, flywheel.getTargetRpm(), 0);
    }

    @Test public void rejectsInvalidEncoderCalibration() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Flywheel(hardware.motor, DcMotorSimple.Direction.FORWARD, invalid, 6000));
            assertThrows(IllegalArgumentException.class,
                    () -> new Flywheel(hardware.motor, DcMotorSimple.Direction.FORWARD, 28, invalid));
        }
    }

    private static final class FakeMotor {
        double power = 1, commandedVelocity, measuredVelocity, current;
        int velocityCommands;
        DcMotor.RunMode mode, pidfMode;
        DcMotor.ZeroPowerBehavior zeroPower;
        DcMotorSimple.Direction direction;
        CurrentUnit currentUnit;
        PIDFCoefficients pidf = new PIDFCoefficients(10, 0, 0, 12);
        final DcMotorEx motor = (DcMotorEx) Proxy.newProxyInstance(DcMotorEx.class.getClassLoader(),
                new Class<?>[]{DcMotorEx.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setPower": power = (double) args[0]; return null;
                        case "setDirection": direction = (DcMotorSimple.Direction) args[0]; return null;
                        case "setZeroPowerBehavior": zeroPower = (DcMotor.ZeroPowerBehavior) args[0]; return null;
                        case "setMode": mode = (DcMotor.RunMode) args[0]; return null;
                        case "setVelocity": commandedVelocity = (double) args[0]; velocityCommands++; return null;
                        case "getVelocity": return measuredVelocity;
                        case "getCurrent": currentUnit = (CurrentUnit) args[0]; return current;
                        case "setVelocityPIDFCoefficients":
                            pidf = new PIDFCoefficients((double) args[0], (double) args[1],
                                    (double) args[2], (double) args[3]); return null;
                        case "getPIDFCoefficients": pidfMode = (DcMotor.RunMode) args[0]; return pidf;
                        default: throw new AssertionError("Unexpected motor call: " + method.getName());
                    }
                });
    }
}
