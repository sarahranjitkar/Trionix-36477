# Easy autonomous movements (Pedro 3.0.1)

`EasyAuto` builds Ivy commands for the existing `PedroAuto` base class. Distances
are inches and public turn arguments are degrees. No drivetrain, tuning, or
Driver Controlled settings are changed.

## Coordinate contract

- Intake is the front. Localization must report that direction as robot +X.
- `moveForward` / `up` / `forward`, `moveBackward` / `down` / `backward`, `left`, and `right` are relative to the
  robot's heading **when that command starts**, and keep that heading.
- East = field +X = 0°, North = field +Y = 90°, West = 180°, South = 270°.
  These are labels on the Pedro field map, not magnetic compass directions.
- Set the starting pose to the actual robot placement and intake heading.
  Setting a pose does not physically orient the robot.
- Positive `turnByDegrees` is counterclockwise viewed from above; negative is
  clockwise. It supports turns beyond 180° and full revolutions, using successive
  heading targets of at most 90°. Large turns may need a longer timeout.
- Absolute turns use the shortest direction. The exact 180° tie direction is
  unspecified; use `turnByDegrees(180)` or `turnByDegrees(-180)` to choose it.

## Example autonomous

Add a class in the `autonomous` package, using the existing `PedroAuto` lifecycle:

```java
package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "My Easy Auto", group = "TRIONIX")
public class MyEasyAuto extends PedroAuto {
    @Override protected Pose startPose() {
        return new Pose(72, 72, 0); // intake faces East; Pose uses radians
    }

    @Override protected Command routine(Follower follower) {
        EasyAuto robot = new EasyAuto(follower);
        return robot.sequence(
                robot.moveForward(12),
                robot.turnNorth(),
                robot.left(6), // now moves toward field West
                robot.pause(0.5),
                robot.turnByDegrees(-45),
                robot.moveBackward(6));
    }
}
```

Calling `robot.up(12)` alone only creates a command. Return a sequence from
`routine`, or schedule the command with Ivy. `PedroAuto` already updates the
follower and scheduler, and stops the drivetrain in `finally`. In another OpMode,
call `follower.update()` then `Scheduler.execute()` every loop and cancel commands
and call `robot.stop()` on exit. Do not use blocking sleeps during movement.

## Methods

| Method | Behavior |
| --- | --- |
| `moveForward(inches)`, `up(inches)`, `forward(inches)` | Toward intake, regardless of field heading |
| `moveBackward(inches)`, `down(inches)`, `backward(inches)` | Away from intake, regardless of field heading |
| `left(inches)`, `right(inches)` | Strafe relative to robot |
| `turnNorth/South/West/East()` | Absolute field heading |
| `turnToDegrees(degrees)` | Any absolute heading |
| `turnByDegrees(degrees)` | Signed relative rotation |
| `moveRobot(forward, left)` | Signed robot-relative offsets, including diagonals |
| `moveField(east, north)` | Signed field-relative offsets |
| `goTo(x, y)` | Straight line to a field coordinate, holding heading |
| `pause(seconds)` | Wait with motors stopped |
| `sequence(commands...)` | Run steps in order |
| `stop()` | Immediately zero drivetrain; cancel the scheduled routine as well |

Directional distances and pauses must be finite and nonnegative; signed offset
methods and turns accept negative values. Zero-distance movement is a hold/settle
command instead of a degenerate path. Paths do not avoid obstacles or enforce
field boundaries. Check the whole route and robot footprint before running it.

Each motion requires measured position within 0.75 inches and heading within 2°
for 0.2 seconds, with path following finished. Motors stop between commands and
at completion; this utility does not blend paths or maintain an active hold during
pauses. An invalid pose or timeout stops motors and throws `MotionFailedException`,
preventing the routine from silently proceeding to its next step. The default
timeout is 10 seconds **per command**, including all legs of a relative turn;
use `new EasyAuto(follower, 15)` for a longer per-command timeout. `PedroAuto`
cleans up and exits on that exception. Custom callers must cancel the routine
if they catch it. Timeouts and numeric checks cannot establish physical
localization accuracy; verify odometry and tuning on the robot.

## Test OpMode

Select **Easy Auto Utility Test** under autonomous / TRIONIX Tests. Place the robot
at field (72,72), intake facing East, with clear space; keep it still during INIT.
This is an interactive test, not a match routine. START alone does not move it.

- D-pad Left/Right selects one action.
- D-pad Up/Down changes distance from 1–48 inches (default 6).
- Bumpers change the relative angle from 15–360° (default 45).
- Release A, then press A to run the selected action once.
- B cancels immediately. Driver Station STOP exits and zeros motors.
- Settings are locked during motion. Telemetry shows selected action, pose, and
  completion, cancellation, or failure. No action automatically repeats.

Test forward and left at 6 inches first; confirm actual intake orientation and
odometry signs. Then test backward/right, the four absolute headings, +45°/-45°,
and larger rotations. Test relative moves again after turning North: forward
should increase field Y and left should decrease field X. Measure physical travel
and inspect telemetry. Finally test B cancellation during a short move. The
coordinate-return menu item runs a direct line to (72,72); ensure that line is clear.

Useful next additions are `goToPose(x,y,heading)` for simultaneous travel/rotation,
named field waypoints, and intake/shoot commands composed with movement. Scoring
commands should reuse the team's calibrated mechanism controls and interlocks.

Reference: [Pedro pose and coordinate setup](https://pedropathing.com/docs/pathing/guide/pose-creation).
