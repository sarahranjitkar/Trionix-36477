package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServoGateTest {
    @Test public void initializesClosedUsingTheSameFullPulseRangeAsCalibration() {
        FakeServo fake = new FakeServo();
        ServoGate gate = new ServoGate(fake.servo, 0.45, 0.68);
        assertEquals(0.45, fake.position, 0);
        assertEquals(1, fake.positionCommands);
        assertTrue(fake.enabled);
        assertEquals(500, fake.range.usPulseLower, 0);
        assertEquals(2500, fake.range.usPulseUpper, 0);
        assertEquals(Servo.Direction.FORWARD, fake.direction);
        assertEquals(0, fake.scaleMin, 0);
        assertEquals(1, fake.scaleMax, 0);
        assertFalse(gate.isOpenCommanded());
    }

    @Test public void openAndCloseUseCalibratedEndpointsInEitherOrder() {
        FakeServo fake = new FakeServo();
        ServoGate gate = new ServoGate(fake.servo, 0.7, 0.3);
        gate.open();
        assertEquals(0.3, fake.position, 0);
        assertEquals(0.3, gate.getCommandedPosition(), 0);
        assertTrue(gate.isOpenCommanded());
        gate.close();
        assertEquals(0.7, fake.position, 0);
        assertFalse(gate.isOpenCommanded());
        gate.setOpen(true);
        assertTrue(gate.isOpenCommanded());
        gate.setOpen(false);
        assertFalse(gate.isOpenCommanded());
    }

    @Test public void uncalibratedOrInvalidEndpointsDoNotWriteToHardware() {
        FakeServo fake = new FakeServo();
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, -0.01, 1.01}) {
            assertThrows(IllegalArgumentException.class, () -> new ServoGate(fake.servo, invalid, 0.7));
            assertThrows(IllegalArgumentException.class, () -> new ServoGate(fake.servo, 0.3, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> new ServoGate(fake.servo, 0.5, 0.5));
        assertEquals(0, fake.writes);
    }

    @Test public void calibrationConfigurationDisablesOnlyThisServoWithoutMovingIt() {
        FakeServo fake = new FakeServo();
        ServoGate.configureServo(fake.servo);
        assertFalse(fake.enabled);
        assertEquals(0, fake.positionCommands);
    }

    @Test public void telemetryLabelsCommandsAndNeverClaimsMeasuredPosition() {
        FakeServo fake = new FakeServo();
        ServoGate gate = new ServoGate(fake.servo, 0.4, 0.6);
        Map<String, Object> values = new HashMap<>();
        Telemetry telemetry = (Telemetry) Proxy.newProxyInstance(Telemetry.class.getClassLoader(),
                new Class<?>[]{Telemetry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addData")) {
                        values.put((String) args[0], args.length == 2 ? args[1] : ((Object[]) args[2])[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected telemetry call: " + method.getName());
                });
        gate.open();
        gate.displayTelemetry(telemetry);
        assertEquals("OPEN", values.get("Gate command"));
        assertEquals(0.6, (double) values.get("Gate commanded position"), 0);
    }

    @Test public void unsupportedControllerIsRejectedBeforeMovement() {
        Servo unsupported = (Servo) Proxy.newProxyInstance(Servo.class.getClassLoader(),
                new Class<?>[]{Servo.class}, (proxy, method, args) -> {
                    throw new AssertionError("Unexpected hardware access: " + method.getName());
                });
        assertThrows(IllegalArgumentException.class, () -> new ServoGate(unsupported, 0.4, 0.6));
        assertThrows(IllegalArgumentException.class, () -> new ServoGate(null, 0.4, 0.6));
    }

    private static final class FakeServo {
        double position, scaleMin, scaleMax;
        int writes, positionCommands;
        boolean enabled = true;
        Servo.Direction direction;
        PwmControl.PwmRange range;
        final Servo servo = (Servo) Proxy.newProxyInstance(Servo.class.getClassLoader(),
                new Class<?>[]{Servo.class, PwmControl.class}, (proxy, method, args) -> {
                    writes++;
                    switch (method.getName()) {
                        case "setPwmDisable": enabled = false; return null;
                        case "setPwmEnable": enabled = true; return null;
                        case "setPwmRange": range = (PwmControl.PwmRange) args[0]; return null;
                        case "setDirection": direction = (Servo.Direction) args[0]; return null;
                        case "scaleRange": scaleMin = (double) args[0]; scaleMax = (double) args[1]; return null;
                        case "setPosition": position = (double) args[0]; positionCommands++; return null;
                        default: throw new AssertionError("Unexpected servo call: " + method.getName());
                    }
                });
    }
}
