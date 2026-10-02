package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import java.lang.reflect.Proxy;
import org.firstinspires.ftc.teamcode.mechanisms.ServoGate;
import org.junit.Test;
import static org.junit.Assert.*;

public class DriverControlledGateTest {
    @Test public void freshOpenPressOpensAndReleaseKeepsItOpen() {
        ServoGate gate = gate();
        DriverControlled.updateGateControls(gate, true, false, false);
        assertTrue(gate.isOpenCommanded());
        DriverControlled.updateGateControls(gate, false, false, true);
        assertTrue(gate.isOpenCommanded());
    }

    @Test public void closeWinsAndReleasingCloseCannotReopenAHeldOpenButton() {
        ServoGate gate = gate();
        gate.open();
        DriverControlled.updateGateControls(gate, true, true, false);
        assertFalse(gate.isOpenCommanded());
        DriverControlled.updateGateControls(gate, true, false, true);
        assertFalse(gate.isOpenCommanded());
        DriverControlled.updateGateControls(gate, false, false, true);
        DriverControlled.updateGateControls(gate, true, false, false);
        assertTrue(gate.isOpenCommanded());
    }

    @Test public void openHeldAtStartDoesNotOpenTheGate() {
        ServoGate gate = gate();
        DriverControlled.updateGateControls(gate, true, false, true);
        assertFalse(gate.isOpenCommanded());
    }

    private static ServoGate gate() {
        Servo servo = (Servo) Proxy.newProxyInstance(Servo.class.getClassLoader(),
                new Class<?>[]{Servo.class, PwmControl.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setPwmDisable":
                        case "setPwmEnable":
                        case "setPwmRange":
                        case "setDirection":
                        case "scaleRange":
                        case "setPosition": return null;
                        default: throw new AssertionError("Unexpected servo call: " + method.getName());
                    }
                });
        return new ServoGate(servo, 0.4, 0.6);
    }
}
