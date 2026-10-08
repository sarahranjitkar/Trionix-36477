package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import static com.pedropathing.api.Paths.line;
import static com.pedropathing.ivy.groups.Groups.sequential;

/**
 * Pedro 3 commands: inches, degrees, intake = front. East = +X/0 degrees,
 * North = +Y/90 degrees. Call follower.update() then Scheduler.execute() each loop.
 * Commands capture the measured pose when STARTED, not when the routine is built.
 * A timeout throws MotionFailedException to abort the routine; always stop in finally.
 */
public final class EasyAuto {
    private final Follower follower;
    private final DoubleSupplier seconds;
    private final double timeout;
    private static final double POSITION_TOLERANCE = 0.75;
    private static final double HEADING_TOLERANCE = Math.toRadians(2);
    private static final double SETTLE_SECONDS = 0.2;

    public EasyAuto(Follower follower) { this(follower, 10); }
    public EasyAuto(Follower follower, double timeoutSeconds) {
        this(follower, timeoutSeconds, () -> System.nanoTime() / 1e9);
    }
    EasyAuto(Follower follower, double timeoutSeconds, DoubleSupplier seconds) {
        finite(timeoutSeconds);
        if (timeoutSeconds <= 0) throw new IllegalArgumentException("Timeout must be positive");
        this.follower = Objects.requireNonNull(follower);
        this.seconds = Objects.requireNonNull(seconds);
        this.timeout = timeoutSeconds;
    }

    public Command up(double inches) { return forward(inches); }
    public Command down(double inches) { return backward(inches); }
    public Command forward(double inches) { distance(inches); return moveRobot(inches, 0); }
    public Command backward(double inches) { distance(inches); return moveRobot(-inches, 0); }
    public Command left(double inches) { distance(inches); return moveRobot(0, inches); }
    public Command right(double inches) { distance(inches); return moveRobot(0, -inches); }

    /** Signed forward/left offsets; supports diagonals while preserving heading. */
    public Command moveRobot(double forwardInches, double leftInches) {
        finite(forwardInches); finite(leftInches);
        return move(p -> robotTarget(p, forwardInches, leftInches));
    }

    /** Signed field offsets, independent of the robot's current heading. */
    public Command moveField(double eastInches, double northInches) {
        finite(eastInches); finite(northInches);
        return move(p -> new Pose(p.x() + eastInches, p.y() + northInches, p.heading()));
    }

    /** Straight line to field coordinates, preserving the current heading. */
    public Command goTo(double xInches, double yInches) {
        finite(xInches); finite(yInches);
        return move(p -> new Pose(xInches, yInches, p.heading()));
    }

    public Command turnNorth() { return turnToDegrees(90); }
    public Command turnSouth() { return turnToDegrees(270); }
    public Command turnWest() { return turnToDegrees(180); }
    public Command turnEast() { return turnToDegrees(0); }

    /** Absolute field heading, using the shortest turn (180-degree tie is unspecified). */
    public Command turnToDegrees(double degrees) {
        finite(degrees);
        return motion(p -> p.withHeading(Math.toRadians(degrees % 360)), false, 0);
    }

    /** Signed relative rotation: positive CCW, negative CW; supports full revolutions. */
    public Command turnByDegrees(double degrees) {
        finite(degrees);
        return motion(Function.identity(), false, degrees);
    }

    /** Pause with motors stopped; does not actively hold position. */
    public Command pause(double secondsToWait) {
        distance(secondsToWait);
        double[] start = new double[1];
        return Command.build().requiring(follower)
                .setStart(() -> { stop(); start[0] = seconds.getAsDouble(); })
                .setDone(() -> seconds.getAsDouble() - start[0] >= secondsToWait)
                .setEnd(reason -> stop());
    }

    public Command sequence(Command... commands) { return sequential(commands); }
    public void stop() { AutoCommands.stop(follower); }

    static Pose robotTarget(Pose p, double forward, double left) {
        double c = Math.cos(p.heading()), s = Math.sin(p.heading());
        return new Pose(p.x() + forward * c - left * s,
                p.y() + forward * s + left * c, p.heading());
    }

    private Command move(Function<Pose, Pose> target) { return motion(target, true, 0); }

    private Command motion(Function<Pose, Pose> targetFactory, boolean translate, double rotation) {
        return new Motion(targetFactory, translate, rotation).command();
    }

    private final class Motion {
        final Function<Pose, Pose> factory;
        final boolean translate;
        final double rotation;
        Pose target;
        double remaining, started, settledSince;
        Motion(Function<Pose, Pose> factory, boolean translate, double rotation) {
            this.factory = factory; this.translate = translate; this.rotation = rotation;
        }
        Command command() {
            return Command.build().requiring(follower).setStart(this::start)
                    .setDone(this::done).setEnd(reason -> stop());
        }
        void start() {
            Pose current = checkedPose();
            target = factory.apply(current);
            remaining = rotation;
            started = seconds.getAsDouble();
            settledSince = Double.NaN;
            if (rotation != 0) advanceTurn();
            else if (translate && current.distance(target) > 1e-6)
                follower.follow(line(current, target).constant(current.heading()));
            else follower.hold(target);
        }
        void advanceTurn() {
            // Under 180 degrees keeps every leg in the requested turn direction.
            double step = Math.copySign(Math.min(90, Math.abs(remaining)), remaining);
            target = target.withHeading(target.heading() + Math.toRadians(step));
            remaining -= step;
            follower.hold(target);
        }
        boolean done() {
            Pose current = checkedPose();
            double now = seconds.getAsDouble();
            if (now - started >= timeout) fail("Motion timed out; target " + target);
            double error = Math.atan2(Math.sin(target.heading() - current.heading()),
                    Math.cos(target.heading() - current.heading()));
            boolean close = current.distance(target) <= POSITION_TOLERANCE
                    && Math.abs(error) <= HEADING_TOLERANCE;
            if (close && remaining != 0) {
                advanceTurn(); settledSince = Double.NaN; return false;
            }
            if (!close || follower.following()) settledSince = Double.NaN;
            else if (Double.isNaN(settledSince)) settledSince = now;
            return !Double.isNaN(settledSince) && now - settledSince >= SETTLE_SECONDS;
        }
    }

    private Pose checkedPose() {
        Pose p = follower.pose();
        if (p == null || !Double.isFinite(p.x()) || !Double.isFinite(p.y())
                || !Double.isFinite(p.heading())) fail("Invalid localization pose");
        return p;
    }
    private void fail(String message) { stop(); throw new MotionFailedException(message); }
    public static final class MotionFailedException extends IllegalStateException {
        MotionFailedException(String message) { super(message); }
    }
    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Value must be finite");
    }
    private static void distance(double value) {
        finite(value);
        if (value < 0) throw new IllegalArgumentException("Value must be nonnegative");
    }
}
