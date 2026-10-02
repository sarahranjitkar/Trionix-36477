package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel;
import org.junit.Test;
import static org.junit.Assert.*;

public class DriverInputsTest {
    private final Gamepad pad = new Gamepad();
    private final DriverControlled.DriverInputs inputs = new DriverControlled.DriverInputs(pad);

    @Test public void intakeTogglesOncePerPressRegardlessOfLoopCount() {
        pad.left_bumper = true;
        repeat(100);
        assertEquals(1, inputs.intakeDirection);
        pad.left_bumper = false;
        repeat(51);
        assertEquals(1, inputs.intakeDirection);
        pad.left_bumper = true;
        repeat(100);
        assertEquals(0, inputs.intakeDirection);
    }

    @Test public void reverseSwitchesDirectionThenTogglesOff() {
        pad.left_bumper = true;
        inputs.update(pad);
        pad.left_bumper = false;
        pad.right_bumper = true;
        repeat(99);
        assertEquals(-1, inputs.intakeDirection);
        pad.right_bumper = false;
        inputs.update(pad);
        pad.right_bumper = true;
        inputs.update(pad);
        assertEquals(0, inputs.intakeDirection);
    }

    @Test public void bothBumpersStopAndRemainingHeldBumperDoesNotRestart() {
        pad.right_bumper = true;
        inputs.update(pad);
        pad.left_bumper = true;
        repeat(100);
        assertEquals(0, inputs.intakeDirection);
        pad.right_bumper = false;
        repeat(100);
        assertEquals(0, inputs.intakeDirection);
    }

    @Test public void holdingRpmButtonsMakesOnlyOneStep() {
        pad.dpad_up = true;
        repeat(1000);
        assertEquals(100, inputs.selectedRpm, 0);
        assertTrue(inputs.launcherRunning);
        pad.dpad_up = false;
        inputs.update(pad);
        pad.dpad_up = true;
        repeat(1000);
        assertEquals(200, inputs.selectedRpm, 0);
        pad.dpad_up = false;
        pad.dpad_down = true;
        repeat(1000);
        assertEquals(100, inputs.selectedRpm, 0);
    }

    @Test public void stopWinsAndResumeUsesRememberedSelection() {
        pad.dpad_up = true;
        inputs.update(pad);
        pad.dpad_up = false;
        inputs.update(pad);
        pad.dpad_up = true;
        pad.dpad_left = true;
        pad.dpad_right = true;
        repeat(100);
        assertFalse(inputs.launcherRunning);
        assertEquals(100, inputs.selectedRpm, 0);
        pad.dpad_right = false;
        repeat(100);
        assertFalse(inputs.launcherRunning);
        assertEquals(100, inputs.selectedRpm, 0);
        pad.dpad_up = false;
        pad.dpad_left = false;
        inputs.update(pad);
        pad.dpad_left = true;
        inputs.update(pad);
        assertTrue(inputs.launcherRunning);
        assertEquals(100, inputs.selectedRpm, 0);
    }

    @Test public void speedSelectionStaysWithinMotorLimitsAndZeroStops() {
        for (int i = 0; i < 65; i++) {
            pad.dpad_up = true;
            inputs.update(pad);
            pad.dpad_up = false;
            inputs.update(pad);
        }
        assertEquals(Flywheel.MAX_RPM, inputs.selectedRpm, 0);
        for (int i = 0; i < 65; i++) {
            pad.dpad_down = true;
            inputs.update(pad);
            pad.dpad_down = false;
            inputs.update(pad);
        }
        assertEquals(0, inputs.selectedRpm, 0);
        assertFalse(inputs.launcherRunning);
    }

    @Test public void buttonsHeldAtStartMustBeReleasedBeforeActivation() {
        pad.left_bumper = true;
        pad.dpad_up = true;
        DriverControlled.DriverInputs heldAtStart = new DriverControlled.DriverInputs(pad);
        for (int i = 0; i < 100; i++) heldAtStart.update(pad);
        assertEquals(0, heldAtStart.intakeDirection);
        assertEquals(0, heldAtStart.selectedRpm, 0);
        assertFalse(heldAtStart.launcherRunning);
    }

    @Test public void oppositeSpeedButtonsCancelAndDoNotCreateAnExtraStepOnRelease() {
        pad.dpad_up = true;
        pad.dpad_down = true;
        repeat(100);
        pad.dpad_down = false;
        repeat(100);
        assertEquals(0, inputs.selectedRpm, 0);
        assertFalse(inputs.launcherRunning);
    }

    private void repeat(int count) {
        for (int i = 0; i < count; i++) inputs.update(pad);
    }
}
