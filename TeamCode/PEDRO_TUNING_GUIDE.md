# TRIONIX — Pedro 3 Tuning Guide

For TRIONIX robot code: FTC SDK 12.0, Pedro 3.0.1, AutoTune 1.0.1, and Ivy 1.1.1.

The team confirmed successful tuning and the upgrade on September 22, 2026. Measured settings are committed in `Constants.java`. Use this guide when retuning; deploy the updated project before starting. The powered Foresight tuners include a Stop-handling correction.

Follow this order:

**Motor directions → Pinpoint → Position verification → Foresight → Autonomous testing**

## 1. Prepare the robot and workspace

- Use the assembled robot with its battery and normal mechanisms installed.
- Check that wheels, odometry pods, and mounting screws are secure. Both odometry wheels must maintain contact with the floor.
- Use your practice tiles and a charged battery.
- Have a tape measure and tape for marking positions.
- Assign one person to the computer and another to the Driver Station and robot.
- Clear enough space for forward, sideways, and rotating movements. Some tests use full power and can coast beyond the requested distance.

Keep the robot stationary while each test initializes so Pinpoint can calibrate.

## 2. Deploy the project

1. Open the `pedro-upgrade` project in Android Studio.
2. Confirm the branch is `upgrade/sdk12-pedro3-ivy`.
3. Connect the powered Control Hub using a USB data cable.
4. Select the Control Hub in Android Studio’s device selector.
5. Keep **TeamCode** selected and click **Run**.
6. Wait for installation to finish.
7. Confirm the Driver Station connects and the robot configuration is active.

The Driver Station should use the matching SDK 12 release. Disconnect the USB cable before driving the robot.

## 3. Open AutoTune

1. Connect the laptop to the robot’s Wi-Fi.
2. Open a web browser on your laptop, such as Chrome or Safari. Type `http://192.168.43.1:10158` into the address bar and press Enter. AutoTune is a webpage hosted by the Control Hub; no separate app is needed. The updated robot app must be running.
3. Look for **Mecanum Tuner**, **Pinpoint Tuner**, **Foresight Tuner**, and **Tests**.

The website controls the tuning sequence. **Treat Continue as permission for the robot to move:** the current tuning software can initialize and start the test automatically.

If the page does not load, check the laptop’s Wi-Fi connection and confirm that the updated app is running on the Control Hub.

## 4. Verify motor names and directions

For this test, support the robot securely with its drive wheels clear of the floor.

1. Select **Mecanum Tuner**.
2. Enter these exact names:

| Position | Name |
|---|---|
| Front left | `Front Left Motor` |
| Front right | `Front Right Motor` |
| Back left | `Back Left Motor` |
| Back right | `Back Right Motor` |

3. Follow the wheel diagram for each motor.
4. Observe which wheel spins and its direction.
5. Stop that test when you have observed enough, then answer the questions.
6. Repeat for all four wheels.

If the wrong wheel spins, correct the hardware configuration or wiring before continuing.

7. Open the results’ **Java** tab.
8. In `Constants.java`, replace the existing `drivetrainConfig` declaration with the generated one.
9. Keep `c.manualBrakeMode.set(true);` inside its configuration block.
10. Build and deploy again.

Place the robot on the floor and briefly run **Driver Controlled**. Verify forward, backward, sideways movement, rotation, and Stop. [Official mecanum instructions](https://pedropathing.com/docs/pathing/tuning/drivetrain/mecanum)

## 5. Tune Pinpoint

The robot must be on the floor with both odometry pods touching the tiles.

1. Select **Pinpoint Tuner**.
2. Enter `pinpoint` as the hardware name.
3. Select **FOUR_BAR**, provided those are still the installed pods.
4. Follow the forward-direction test: push the robot straight forward, then stop the test.
5. Follow the lateral-direction test: push the robot straight **left**, then stop it.
6. For offsets, rotate the robot **180 degrees counterclockwise**, as viewed from above. Rotate about its center without sliding it sideways.
7. Stop the test after completing the rotation.
8. Copy the generated Java configuration.
9. Replace the entire `localizerConfig` declaration in `Constants.java`.
10. Build and deploy again.

The forward pod connects to Pinpoint’s X port; the sideways pod connects to its Y port. Select Custom only if the installed pods actually require custom resolution calibration. [Official Pinpoint instructions](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint)

## 6. Verify position measurements before powered calibration

1. Open **Tests**.
2. Explicitly select **Pose Test**. The menu defaults to Line Test, which needs Foresight tuning.
3. Start with the robot stationary.
4. Once the test is running, manually push it forward a measured 24 inches.
5. Watch `Pose` in Driver Station telemetry: X should increase by approximately 24 inches. This test displays live readings rather than generating calibration constants to copy.
6. Restart the test and push it left 24 inches: Y should increase by approximately 24 inches.
7. Restart and rotate counterclockwise 90 degrees: heading should change by approximately +1.57 radians.
8. Return to the original marked position and orientation; the readings should return near their starting values.

These directions assume the test’s starting heading of zero.

Stop here if distance, direction, or rotation is clearly wrong. Check pod selection, wiring, floor contact, and offsets before running Foresight.

## 7. Run Foresight AutoTune

This stage measures acceleration, braking, and correction behavior. The robot moves itself.

1. Select **Foresight Tuner**.
2. Read each movement description before continuing.
3. Position the robot with clear travel in the specified direction.
4. Use the starting values below only when the available space accommodates them, including extra stopping room:

| Prompt | Current default |
|---|---:|
| Velocity-identification travel distance | 48 inches |
| Deceleration-identification target velocity | 30 inches/second |
| Braking-identification travel distance | 36 inches |

The braking distance must be at least 15 inches. The Velocity field means inches **per second**, despite the shorter wording in the current prompt.

The sequence measures:

- Forward and sideways maximum velocity
- Forward and sideways natural deceleration
- Heading braking and heading correction
- Forward and sideways braking
- Forward and sideways position correction

Some tests travel back and forth or repeat at different powers. Reposition only between completed tests when prompted. If you stop or interrupt a calibration, rerun it rather than using incomplete results. [Official Foresight instructions](https://pedropathing.com/docs/pathing/tuning/foresight)

## 8. Save the Foresight results

1. Open the completed procedure’s **Java** tab.
2. Copy the entire generated `ForesightConfig` declaration.
3. Find the existing `public static ForesightConfig foresightConfig` declaration in `Constants.java`.
4. Replace the entire declaration with the generated declaration, including all its coefficients.
5. Save, build, and deploy.

**AutoTune does not automatically update your Android Studio files.** Each generated configuration must be copied into the code and redeployed.

Do not duplicate declarations or replace the entire `Constants.java` file. Preserve the other settings.

## 9. Validate the tuned robot

Run these progressively:

| Test | What to check |
|---|---|
| Driving / Localization Test | Correct controls and believable position readings |
| Hold Test | Stable position and heading without persistent oscillation |
| Line Test | Straight travel and consistent stopping |
| Curve / Interpolation Test | Smooth travel while maintaining or changing heading |
| **24 Inch Movement Test** | Your first short autonomous route |
| **Red Park Auto** | The full retained parking route |

Follow each test’s movement description. Some tests repeat until stopped.
The current Line, Curve, and Interpolation tests use a fixed 48-inch distance, ignoring the browser’s Distance entry. Clear enough space for that travel, the robot footprint, and stopping.

Before parking, confirm that its stored field coordinates and robot clearance match your actual setup. Physically place the robot at the intended starting position and orientation—the code assigning a starting pose does not locate the robot automatically.

During a short movement test, verify that the Driver Station’s Stop button stops motion. [Official test guide](https://pedropathing.com/docs/pathing/tuning/test)

## 10. Record results and keep the successful settings

For each repeat run, record:

| Run | Battery voltage | Final position error | Heading error | Duration | Notes |
|---|---|---|---|---|---|
| 1 | | | | | |
| 2 | | | | | |
| 3 | | | | | |
| 4 | | | | | |
| 5 | | | | | |

As a practical team check, aim for five consecutive satisfactory runs using the same starting placement. Define “satisfactory” by the clearance or alignment your routine actually needs.

Commit the measured configuration after validation. Revisit tuning when robot weight, wheels, gearing, pod placement, or floor conditions change significantly.

## Settings file

In the upgrade project, edit:

`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Constants.java`

The current local project is `/Users/saunakranjitkar/tmp/trionix/pedro-upgrade`.
