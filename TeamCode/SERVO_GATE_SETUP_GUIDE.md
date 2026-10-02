# Servo gate setup and calibration

The POLLEN gate uses two measured servo positions: CLOSED blocks the passage to the launcher; OPEN clears it. `ServoGate` provides `open()` and `close()`. No external PID tuning is needed because the positional servo controls its own shaft position.

## Parts in the inventory

Checked October 2, 2026. Both `06_Build_Team/Trionix_Parts_Inventory.xlsx` and `Trionix_Parts_Inventory (1).xlsx` agree on these entries in the Inventory sheet:

| Item | Part number | Recorded quantity | Row |
| --- | --- | --- | --- |
| 2000 Series Torque Servo, 25-2 | 2000-0025-0002 | 2 | 11 |
| 2000 Series Speed Servo, 25-3 | 2000-0025-0003 | 2 | 12 |
| Dual Mode Servo Programmer | 3102-0001-0001 | 1 | 13 |
| Compact ServoBlock, H25T | 3217-0001-2501 | 3 | 14 |
| Standard servo frame, 43 mm | 1802-0043-0001 | 1 | 15 |
| Servo hub, 25T spline, 32 mm | 1908-0025-0032 | 1 | 16 |
| Servo extensions, 300 / 600 mm | 3802-1718-0300 / -0600 | 2 each | 17–18 |

The used/missing and storage fields are blank for these entries, so these are recorded quantities, not verified spare counts. Check the actual servo label and availability before mounting.

Both listed servos work with this code in **Servo Mode**. A lightly loaded gate can use the speed model for quicker movement; the torque model offers more holding force. Mechanical load and binding must be checked on the actual gate. The code uses the manufacturer-supported 500–2500 microsecond PWM range for both models.

## 1. Set positional mode

1. Keep the launcher and intake off and remove POLLEN. For the first center command, disconnect the gate linkage or remove the horn so the shaft can move without hitting the mechanism.
2. Disconnect the servo from the Hub. Connect it and the supplied battery holder to the goBILDA **3102-0001-0001** programmer, observing the printed polarity labels. Use the programmer's specified supply, never the robot's 12 V battery directly.
3. Slide the programmer's mode switch to **S** (Servo Mode), then briefly press **P**. The LEDs flash to confirm programming. Do not select C, which controls continuous rotation rather than holding a position.
4. Disconnect the programmer. With robot power off, connect the servo to an unused REV Hub **servo port**, matching signal, positive and ground markings. An extension can be used if needed.

Source: [goBILDA programmer instructions](https://www.gobilda.com/servo-programmer-for-2000-series-dual-mode-servo/).

## 2. Configure the Hub and deploy

1. In the Driver Station robot configuration, select the Hub and the servo port you used. Add a standard positional **Servo**, not a continuous rotation servo.
2. Name it exactly **Servo Gate**, including the space and capitalization. Alternatively, change `ServoGate.SERVO_NAME` to match your preferred configured name.
3. Save and activate the robot configuration, then deploy the project from Android Studio.
4. Select **Servo Gate Calibration** under TeleOp. This OpMode only controls the gate servo; it does not command launcher or intake motors.

## 3. Center the shaft and mount the gate

1. With the horn/linkage disconnected, press INIT and START. The tuner leaves the servo output disabled until you press **A** after START.
2. Press A. On a fresh tuner run this commands **0.500**, the middle of the configured pulse range. The servo should move to a position and hold. If it keeps rotating, stop and repeat Servo Mode programming.
3. Press STOP and switch robot power off. Without turning the shaft, fit the horn so the gate is roughly midway between the desired open and closed positions. Secure the horn and linkage. This leaves adjustment room on both sides.
4. Restart the tuner, then press A. Keep the gate path clear while it returns to center. If mounting at center cannot give clearance throughout the needed travel, change the horn indexing/linkage before continuing.

`0.0` and `1.0` are the limits of the configured servo command range, not automatically CLOSED and OPEN. With these servo models and this pulse range, full travel is approximately 300 degrees. A 0.010 step is about 3 degrees at the shaft; 0.002 is about 0.6 degrees. Gate angle can differ due to linkage geometry. Do not assume that 0.0 or 1.0 is mechanically reachable with your gate attached.

## 4. Record CLOSED

1. Tap D-pad LEFT or RIGHT to move by **0.010 per press**. Observe which direction closes your gate; closed can be either the higher or lower number.
2. Press LEFT BUMPER to switch to fine steps of **0.002** near the desired endpoint.
3. Find a position that blocks the POLLEN passage without driving against a hard stop or squeezing the mechanism. If it binds or strains, press BACK to release the servo output, then remove the obstruction before enabling again. BACK removes holding torque; it does not hold the gate still.
4. Press **X** to record the displayed command as CLOSED. This records the value in the running tuner only.

## 5. Record OPEN

1. Use the same D-pad adjustments to move until the passage clears the POLLEN without interference.
2. Press **Y** to record OPEN. The two values must differ.
3. Press **B** to recall CLOSED and **RIGHT BUMPER** to recall OPEN. Repeat several times, allowing the gate to finish moving. B takes priority if both recall controls are held.
4. With the launcher off, verify that CLOSED retains POLLEN and OPEN allows it through. Confirm that neither endpoint binds under normal loading.
5. Write down both displayed values before STOP. STOP releases PWM and does not close or mechanically lock the gate.

## 6. Save the two positions in code

Open `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/mechanisms/ServoGate.java`. Replace the two `Double.NaN` placeholders with your recorded numbers:

```java
public static final double CLOSED_POSITION = /* your measured CLOSED value */;
public static final double OPEN_POSITION = /* your measured OPEN value */;
```

The example above shows where the numbers go; enter actual numeric values before building. Do not paste invented example endpoints. `Double.NaN` deliberately prevents normal gate initialization until calibration is supplied.

Deploy again. The tuner loads your saved endpoints; use A to enable, then B / RIGHT BUMPER to verify each endpoint. A still begins at 0.500 on a fresh tuner run. X/Y edits are not written to Java or automatically retained after the tuner ends. Repeat calibration if you change the horn indexing, linkage, servo direction or PWM range.

## Controls at a glance

| Gamepad 1 control | Action |
| --- | --- |
| A | Enable at displayed position; initially 0.500 |
| D-pad LEFT / RIGHT | Nudge position down / up once per press |
| LEFT BUMPER | Toggle 0.010 / 0.002 adjustment step |
| X / Y | Record CLOSED / OPEN for this session |
| B / RIGHT BUMPER | Recall recorded CLOSED / OPEN |
| BACK | Disable gate PWM; releases holding torque; takes priority |
| Driver Station STOP | End calibration and disable gate PWM |

After BACK, A resumes at the displayed position. Adjustment/record/recall commands are ignored while disabled. A missing recorded endpoint cannot be recalled.

## Using ServoGate in robot code

After calibration:

```java
ServoGate gate = new ServoGate(hardwareMap); // Immediately commands calibrated CLOSED.
gate.open();                               // Allow POLLEN toward the launcher.
gate.close();                              // Block POLLEN.
gate.setOpen(false);                        // Equivalent to close().
gate.displayTelemetry(telemetry);
telemetry.update();
```

Import `org.firstinspires.ftc.teamcode.mechanisms.ServoGate`.

Calls return immediately while the servo moves. `isOpenCommanded()` and `getCommandedPosition()` report what the code requested, not measured gate position. Allow physical travel time before feeding. The standard three-wire connection cannot tell the Hub whether the servo actually arrived or a POLLEN jam prevented it.

The class starts CLOSED after valid calibration. Keep the gate closed until feeding is intended and the launcher is ready. End feeding and command CLOSED before normal operation ends while the mechanism still has time/power to move. A shutdown, STOP, or loss of power is not a guarantee that the gate closes or stays held; that requires a suitable mechanical return/latch if needed.

## Driver Controlled gate controls

After recording and deploying both calibrated endpoints, select **Driver Controlled**:

- INIT commands CLOSED immediately. With unset/invalid endpoints, initialization fails with a calibration message; run **Servo Gate Calibration** first.
- Press **gamepad 1 A** to command OPEN. It stays open until you close it.
- Press **gamepad 1 B** to command CLOSED. B takes priority when both buttons are pressed.
- A must be released and pressed again after START or after B closes the gate while A is held. Releasing B alone does not reopen it.
- Telemetry shows the commanded gate state and position. These are not physical position measurements.
- When TeleOp ends, the code requests CLOSED and stops the motors. STOP/power loss can prevent the servo from completing that movement; close it before ending normal operation.

The driver decides when to feed. These buttons do not start the launcher, check its RPM, or meter one POLLEN at a time. Autonomous routines are unchanged.

The intake and launcher controls in the same TeleOp respond once per button press:

- LEFT BUMPER toggles intake forward/off; RIGHT BUMPER toggles reverse/off. Pressing the other bumper switches direction. Both together stop the intake.
- D-pad UP/DOWN changes the selected launcher speed by exactly 100 RPM per press, bounded to 0–6000 RPM. As before, a speed adjustment commands the launcher to run at the new selection; zero stops it. Holding a button does not repeat.
- D-pad RIGHT stops the launcher and retains the selected RPM. It takes priority over run and speed changes. D-pad LEFT resumes that saved selection.
- Buttons held through START or during a conflicting stop command must be released and pressed again before they activate. Both RPM adjustment buttons together make no change.

## References

- [goBILDA 25-2 torque servo](https://www.gobilda.com/2000-series-dual-mode-servo-25-2-torque/)
- [goBILDA 25-3 speed servo](https://www.gobilda.com/2000-series-dual-mode-servo-25-3-speed/)
- [goBILDA dual mode programmer](https://www.gobilda.com/servo-programmer-for-2000-series-dual-mode-servo/)
- [FTC Servo API and commanded-position semantics](https://javadoc.io/static/org.firstinspires.ftc/RobotCore/7.1.0/com/qualcomm/robotcore/hardware/Servo.html)
