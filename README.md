# TRIONIX #36477 Robot Code

Fresh FTC SDK 12.0 / Pedro Pathing 3.0.1 / AutoTune 1.0.1 / Ivy 1.1.1 foundation.
Java, mecanum drive, goBILDA Pinpoint, two goBILDA four-bar odometry pods.

**Status: software foundation, not robot-validated.** TeleOp and AutoTune are available.
Autonomous deliberately remains inactive until the robot's Foresight configuration is entered.
No SDK/device updates or tuning runs have been performed on the robot by this change.

## Open and build

Use Android Studio Narwhal 3 Feature Drop or later, with its bundled JDK, and open this
repository root. Let Gradle sync and select the TeamCode configuration to build/deploy.
The project includes the SDK's Gradle wrapper and signing configuration.

```sh
./gradlew :TeamCode:assembleDebug :TeamCode:testDebugUnitTest
```

The debug APK is produced in `TeamCode/build/outputs/apk/debug/` (not committed).
Keep development copies and all generated files outside the shared TRIONIX project folder,
under `~/tmp/trionix`. Do not check in local SDK paths, build output, or tuning logs.

Deploy the robot app through Android Studio. Update the Driver Station app to 12.0 as well.
Installing the stock Robot Controller APK afterward replaces this app and its embedded team code.
Control Hub OS and hub firmware are separate from the SDK/app versions; check their status separately.

## Files students will edit

- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Constants.java`: shared
  drive configuration, Pinpoint configuration, intake name/power, and Foresight tuning.
- `teleop/DriverControlled.java`: robot-centric driver controls.
- `mechanisms/Intake.java`: intake behavior; add other mechanism classes when hardware is defined.
- `autonomous/RedAutoPark.java`: the existing two-segment route in the Pedro 3 path API.
- `autonomous/MovementTest.java`: a 24-inch straight path for the first autonomous check.
- `autonomous/PedroAuto.java`: starting pose, follower/scheduler loop, and cleanup.
- `pedro/Tuning.java`: registers the four relevant tuning/test procedures.

The shortened paths above are relative to the same teamcode package.

## Hardware carried forward

| Device | Robot configuration name | Direction |
|---|---|---|
| Front left drive | Front Left Motor | REVERSE |
| Back left drive | Back Left Motor | REVERSE |
| Front right drive | Front Right Motor | FORWARD |
| Back right drive | Back Right Motor | FORWARD |
| Intake | Intake Motor | FORWARD; 0.5 power |
| Odometry computer | pinpoint | X/forward pod REVERSED; Y/strafe pod FORWARD |

Motor names/directions and pod settings come from repository `main` at `be0e16d`.
The old forward pod Y offset (-7.6043422428641705 in) becomes `xPodOffset`;
the old strafe pod X offset (0.9393324964628462 in) becomes `yPodOffset`.
These describe each pod's perpendicular distance from the rotation center, not interchangeable axes.
Recheck the offsets and directions on the current chassis. Old mass/PID/velocity/braking settings
were intentionally not converted into fabricated Pedro 3 tuning values.

TeleOp preserves the left-stick drive, right-stick turn, left-bumper intake-on and
right-bumper intake-off controls. Right bumper wins if both are pressed. Stop zeros
both drive and intake. TeleOp does not require a tuned follower or Pinpoint initialization.

## First robot session

1. Verify the active robot configuration matches the table. Check the four motor directions,
   then forward, strafe-right, and clockwise rotation in Driver Controlled. Check intake and Stop.
2. Connect the computer to the robot Wi-Fi and open `http://192.168.43.1:10158`.
   Run **Mecanum Tuner**, then **Pinpoint Tuner**. Copy their generated configurations into
   the matching declarations in `pedro/Constants.java`, retaining manual brake mode for TeleOp.
3. Build/deploy again. Use **Tests** to verify localization: push forward/left and rotate;
   compare displayed motion with physical motion. Before Foresight tuning, follower-dependent
   tests are absent, but drivetrain/localization tests remain available.
4. Run **Foresight Tuner** in clear field space, following its prompts. Replace
   `public static ForesightConfig foresightConfig = null;` with its complete Java output.
   The imports needed by the generated controllers, matrix, and vector values are included.
   Do not paste another robot's example coefficients. Build/deploy again.
5. Use the tuning tests for heading/line/curve checks, then **24 Inch Movement Test**:
   its start is (24, 24, 0 degrees), and its end is (48, 24, 0 degrees), in inches.
   Test Stop during motion before running a full route.
6. Verify field coordinates and robot clearance for **Red Park Auto** before running it:
   (100, 8, 90 degrees) -> (100, 30, 90 degrees) -> (47, 8, 90 degrees).
   These are preserved team coordinates, not a new field or scoring validation.
7. Repeat parking runs and record endpoint error, heading, duration, and battery condition.
   Merge into main only after TeleOp, intake, localization, autonomous, and Stop checks pass.

`Constants.create()` refuses an untuned follower; autonomous displays an explanation and
returns without driving. A non-null config enables autonomous, so fill in the complete
measured configuration. This flag does not certify physical calibration or field validation.
Revisit tuning after significant weight, gearing, wheel, or pod changes.

## Autonomous lifecycle

Every autonomous explicitly seeds its starting pose after Pinpoint calibration. The loop
updates the follower, then the Ivy scheduler. Ivy 1.1.1's built-in `follow` command finishes
at parametric completion, so `AutoCommands.followAndSettle` extends its completion condition
to wait for endpoint correction (including Pedro's configured timeout). This is not a guarantee
that a scoring tolerance was reached: log/measure endpoint error before scoring actions.
The command claims the follower as a requirement and stops the drive immediately on interruption.
The OpMode also cancels commands and directly stops the drive in `finally`; final position
holding continues after normal route completion until Stop is pressed.

Focused JVM tests cover sequential completion, cancellation, natural holding, and drive conflicts.
They do not simulate localization, motor wiring, tuning, or real motion.

## Baseline and upstream

- Original code: tag `baseline/before-sdk12-pedro3` at `be0e16d`.
- Rebuild branch: `upgrade/sdk12-pedro3-ivy`.
- Foundation and relevant tuner sources: Pedro Quickstart commit `79670fb9c432a3f5342054d69358f3826ac07f1c`.
- The old long movement experiment and unused Motor/Servo examples remain available in Git history.
- Existing local uncommitted changes were not overwritten or included in this branch.

References: [FTC SDK 12](https://github.com/FIRST-Tech-Challenge/FtcRobotController/releases/tag/v12.0),
[Pedro tuning](https://pedropathing.com/docs/pathing/tuning),
[Ivy](https://pedropathing.com/docs/ivy),
[Pedro Quickstart](https://github.com/Pedro-Pathing/Quickstart).
