# Flywheel control and PIDF tuning

For the TRIONIX goBILDA 5203 Yellow Jacket **1:1, 6000 RPM, 8 mm REX** launcher motor. This is a separate Driver Station tuner; Pedro AutoTune tunes the drivetrain, not this launcher.

## Hardware setup

1. Connect both motor power and its encoder to the matching motor/encoder port on the REV Hub.
2. Select the goBILDA 6000 RPM / 1:1 motor type in the robot configuration. The code uses **28 encoder counts per motor output-shaft revolution**. RPM is motor-shaft RPM; external belt or gear ratios are not included.
3. Name the motor **Flywheel Motor**, or change `Flywheel.MOTOR_NAME` to match your existing name. This name has not yet been confirmed on the robot.
4. Deploy the project and select **Flywheel PIDF Tuning** under TeleOp. Keep the launcher clear and unloaded for the initial test.

The 6000 RPM specification is no-load speed at 12 V, not a guaranteed attainable target with a flywheel attached. The software clamps commands to 0–6000 RPM. Start at the tuner's 1000 RPM setting and increase gradually.

## Driver Station controls

Press INIT, then START. The motor stays off until you press A after START.

| Gamepad 1 control | Action |
| --- | --- |
| A | Run at the selected RPM |
| B | Remove power; wheel coasts down. Stop takes priority over A. |
| D-pad up / down | Increase / decrease selected RPM by 100 |
| Left / right bumper | Select the P, I, D, or F gain |
| D-pad left / right | Decrease / increase the selected gain |
| Y | Cycle gain adjustment size through 0.01, 0.1, and 1.0 |
| Driver Station STOP | End the tuner and remove motor power |

Controls act once per press. RPM and gain changes apply immediately while running. While off, adjusting values does not start the motor. Selecting zero RPM turns it off; press A again to restart after raising the selection.

Telemetry shows selected RPM, commanded RPM, measured RPM, RPM error, motor current in amps, and all four gains. A negative measured RPM while commanded forward needs investigation; the code deliberately does not hide it with an absolute value. If the motor spins but measured RPM stays zero, stop and fix the encoder connection before tuning. If the physical launch direction is wrong, stop and change `Flywheel.DIRECTION`, then redeploy.

## Tune and save

1. Begin with the existing Hub gains displayed by the tuner. `Flywheel.TUNED_PIDF` is initially `null`, so the class leaves those gains unchanged. These are starting values, not robot-validated tuning.
2. Run at a modest attainable RPM. Allow spin-up, then watch the measured RPM and error. Confirm direction and encoder readings before increasing speed.
3. **F (feedforward)** supplies the baseline command for speed. If tuning from scratch, start I and D at zero and adjust F with P low, using an attainable middle RPM. Increase F if speed is consistently low; decrease it if high. Do not chase an unattainable speed with larger gains.
4. **P (proportional)** corrects speed error. Increase in small steps to improve recovery after a speed change or launch. If speed oscillates, reduce P. Judge the settled speed, not just the initial spin-up.
5. **I (integral)** may reduce persistent error after F and P are reasonable. Add sparingly; excessive I can cause slow oscillations and overshoot. Leave it at zero if unnecessary.
6. **D (derivative)** may help damp overshoot but can amplify encoder noise. Leave it at zero unless testing demonstrates an improvement.
7. Verify at several intended launch speeds and after normal launch loads. Watch current and RPM recovery, and repeat with different battery charge levels. Stop if the wheel jams or current stays unusually high. Current is displayed for observation; the class does not implement an automatic current cutoff.
8. Press B. Copy the displayed `new PIDFCoefficients(p, i, d, f);` into `Flywheel.TUNED_PIDF`, replacing `null`. Redeploy and verify the saved values. Tuner edits are **not saved automatically**, and Hub PIDF changes do not survive a power cycle.

This is the FTC Hub's velocity PIDF loop using encoder ticks per second. Gains from Pedro or a different software PID implementation are not interchangeable. No additional PID library or repeated `update()` call is needed.

## Using the class in another OpMode

```java
Flywheel flywheel = new Flywheel(hardwareMap);
try {
    waitForStart();
    if (isStopRequested()) return;
    flywheel.setTargetRpm(3000); // Starts immediately; example, not a tuned launch speed.
    while (opModeIsActive()) {
        flywheel.displayTelemetry(telemetry);
        telemetry.update();
        idle();
    }
} finally {
    flywheel.stop();
}
```

Import `org.firstinspires.ftc.teamcode.mechanisms.Flywheel`.

- `setTargetRpm(rpm)` immediately commands a speed, bounded to 0–6000.
- `increaseRpm()` / `decreaseRpm()` change the command by 100 RPM.
- `increaseRpm(amount)` / `decreaseRpm(amount)` use a custom nonnegative step. Increasing from zero starts the motor; use button edge detection when calling these from TeleOp.
- `getCurrentRpm()` and `getCurrentAmps()` return measured values.
- `displayTelemetry(telemetry)` adds readings; the OpMode calls `telemetry.update()`.
- `setPIDF(p, i, d, f)` changes velocity gains; `getPIDF()` reads them.
- `isAtTargetRpm(tolerance)` checks the instantaneous error and returns false when stopped. For automatic feeding, require the speed to remain within tolerance for a suitable dwell time.
- `stop()` removes power and clears the commanded RPM. The wheel coasts and can still report nonzero RPM.

The tuner is the only new registered OpMode. Integrate the class into match TeleOp/autonomous when the motor is tuned and the desired controls/feeding behavior are decided.

## References

- [goBILDA motor specifications](https://www.gobilda.com/5203-series-yellow-jacket-motor-1-1-ratio-24mm-length-8mm-rex-shaft-6000-rpm-3-3-5v-encoder/)
- [FTC motor modes and encoders](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-motor-modes/tech-tip-motor-modes.html)
- [FTC changing PIDF coefficients](https://ftc-docs.firstinspires.org/en/latest/programming_resources/shared/pidf_coefficients/pidf-coefficients.html)
